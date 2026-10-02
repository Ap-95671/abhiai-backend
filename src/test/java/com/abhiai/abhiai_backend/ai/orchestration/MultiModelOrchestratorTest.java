package com.abhiai.abhiai_backend.ai.orchestration;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.*;
import com.abhiai.abhiai_backend.ai.*;
import com.abhiai.abhiai_backend.ai.pipeline.ResponseProcessor;
import com.abhiai.abhiai_backend.exception.*;
import tools.jackson.databind.ObjectMapper;
import static com.abhiai.abhiai_backend.ai.orchestration.PipelineTestSupport.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MultiModelOrchestratorTest {
    private ExecutorService executor;
    private MultiModelOrchestrator orchestrator;
    private Map<String, ModelProvider> providers;
    private final AiModelDefinition openai = registry().find("openai:gpt-test").orElseThrow();
    private final AiModelDefinition gemini = registry().find("gemini:gemini-test").orElseThrow();
    private final AiModelDefinition claude = registry().find("anthropic:claude-test").orElseThrow();

    @BeforeEach void setup() {
        executor = Executors.newFixedThreadPool(3);
        orchestrator = new MultiModelOrchestrator(executor, new ProviderHealthTracker(), new ResultAggregator(),
                new ResponseSynthesizer(new ObjectMapper()), new ResponseProcessor());
        providers = Map.of("openai", mock(ModelProvider.class), "gemini", mock(ModelProvider.class), "anthropic", mock(ModelProvider.class));
    }
    @AfterEach void shutdown() throws Exception { executor.shutdownNow(); assertThat(executor.awaitTermination(2, TimeUnit.SECONDS)).isTrue(); }
    private ExecutionPlan plan(ExecutionStrategy strategy, Duration timeout, ExecutionPlan.Assignment... stages) {
        return new ExecutionPlan(strategy, List.of(stages), openai, timeout, Duration.ofSeconds(3), true);
    }
    private ExecutionPlan.Assignment solver(AiModelDefinition model) { return new ExecutionPlan.Assignment(model, ModelRole.PRIMARY_SOLVER); }
    private AiCompletion run(ExecutionPlan plan) {
        return orchestrator.execute(processor().process(request("Design a complex distributed architecture")), plan, providers);
    }
    private boolean role(AiChatRequest input, ModelRole role) { return input.messages().stream().anyMatch(m -> m.content().startsWith(role.instruction())); }

    @Test void parallelRunsConcurrentlyAndReturnsOnlySynthesisWithUsage() throws Exception {
        var bothStarted = new CountDownLatch(2);
        when(providers.get("openai").generate(any())).thenAnswer(call -> {
            AiChatRequest input = call.getArgument(0);
            if (role(input, ModelRole.SYNTHESIZER)) return new AiCompletion("Unified answer", null, null, null, 20, 7, 0, false);
            bothStarted.countDown();
            assertThat(bothStarted.await(1, TimeUnit.SECONDS)).isTrue();
            return new AiCompletion("First solution", null, null, null, 10, 4, 0, false);
        });
        when(providers.get("gemini").generate(any())).thenAnswer(call -> {
            bothStarted.countDown();
            assertThat(bothStarted.await(1, TimeUnit.SECONDS)).isTrue();
            return new AiCompletion("Different solution", null, null, null, 12, 5, 0, false);
        });
        var result = run(plan(ExecutionStrategy.MULTI_MODEL_PARALLEL, Duration.ofSeconds(2), solver(openai), solver(gemini)));
        assertThat(result.content()).isEqualTo("Unified answer");
        assertThat(result.inputTokens()).isEqualTo(42);
        assertThat(result.outputTokens()).isEqualTo(16);
        assertThat(result.fallbackUsed()).isFalse();
        verify(providers.get("openai"), times(2)).generate(any());
    }
    @Test void oneProviderFailureReturnsUsefulAnswer() {
        when(providers.get("openai").generate(any())).thenReturn(new AiCompletion("Useful answer"));
        when(providers.get("gemini").generate(any())).thenThrow(new AiProviderException("quota", AiProviderFailureKind.RATE_LIMIT));
        var result = run(plan(ExecutionStrategy.MULTI_MODEL_PARALLEL, Duration.ofSeconds(1), solver(openai), solver(gemini)));
        assertThat(result.content()).isEqualTo("Useful answer");
        assertThat(result.fallbackUsed()).isTrue();
        verify(providers.get("openai"), times(1)).generate(any());
    }
    @Test void oneTimeoutIsCancelledAndSurvivingAnswerIsReturned() throws Exception {
        var interrupted = new CountDownLatch(1);
        when(providers.get("openai").generate(any())).thenReturn(new AiCompletion("Useful answer"));
        when(providers.get("gemini").generate(any())).thenAnswer(call -> {
            try { new CountDownLatch(1).await(); }
            catch (InterruptedException cancelled) { interrupted.countDown(); Thread.currentThread().interrupt(); }
            throw new AiProviderException("cancelled");
        });
        var result = run(plan(ExecutionStrategy.MULTI_MODEL_PARALLEL, Duration.ofMillis(150), solver(openai), solver(gemini)));
        assertThat(result.content()).isEqualTo("Useful answer");
        assertThat(interrupted.await(1, TimeUnit.SECONDS)).isTrue();
    }
    @Test void reviewReceivesDraftAndSynthesisReceivesCritiqueAsUntrustedUserEvidence() {
        when(providers.get("openai").generate(any())).thenAnswer(call -> {
            AiChatRequest input = call.getArgument(0);
            if (role(input, ModelRole.SYNTHESIZER)) {
                assertThat(input.messages().getLast().content()).contains("Primary draft", "Specific correction", "Untrusted candidate evidence");
                return new AiCompletion("Correct final answer");
            }
            return new AiCompletion("Primary draft");
        });
        when(providers.get("gemini").generate(any())).thenAnswer(call -> {
            AiChatRequest input = call.getArgument(0);
            assertThat(role(input, ModelRole.REVIEWER)).isTrue();
            assertThat(input.messages().getLast().content()).contains("Primary draft");
            return new AiCompletion("Specific correction");
        });
        assertThat(run(plan(ExecutionStrategy.MULTI_MODEL_REVIEW, Duration.ofSeconds(1), solver(openai),
                new ExecutionPlan.Assignment(gemini, ModelRole.REVIEWER))).content()).isEqualTo("Correct final answer");
    }
    @Test void synthesisFailureReturnsDraftNeverRawCriticism() {
        when(providers.get("openai").generate(any())).thenReturn(new AiCompletion("Primary draft"))
                .thenThrow(new AiProviderException("synthesis failed"));
        when(providers.get("gemini").generate(any())).thenReturn(new AiCompletion("Criticism only"));
        var result = run(plan(ExecutionStrategy.MULTI_MODEL_REVIEW, Duration.ofSeconds(1), solver(openai),
                new ExecutionPlan.Assignment(gemini, ModelRole.REVIEWER)));
        assertThat(result.content()).isEqualTo("Primary draft");
        assertThat(result.fallbackUsed()).isTrue();
    }
    @Test void failedPrimaryPromotesPlannedReviewerToIndependentSolver() {
        when(providers.get("openai").generate(any())).thenThrow(new AiProviderException("failed"));
        when(providers.get("gemini").generate(any())).thenAnswer(call -> {
            assertThat(role(call.getArgument(0), ModelRole.PRIMARY_SOLVER)).isTrue();
            return new AiCompletion("Independent complete answer");
        });
        assertThat(run(plan(ExecutionStrategy.MULTI_MODEL_REVIEW, Duration.ofSeconds(1), solver(openai),
                new ExecutionPlan.Assignment(gemini, ModelRole.REVIEWER))).content()).isEqualTo("Independent complete answer");
    }
    @Test void specializedReviewsRunConcurrentlyAfterPrimary() throws Exception {
        var bothReviewing = new CountDownLatch(2);
        when(providers.get("openai").generate(any())).thenReturn(new AiCompletion("Draft"), new AiCompletion("Final"));
        for (String name : List.of("gemini", "anthropic")) when(providers.get(name).generate(any())).thenAnswer(call -> {
            AiChatRequest input = call.getArgument(0);
            assertThat(input.messages().getLast().content()).contains("Draft");
            bothReviewing.countDown();
            assertThat(bothReviewing.await(1, TimeUnit.SECONDS)).isTrue();
            return new AiCompletion(name + " findings");
        });
        var result = run(plan(ExecutionStrategy.MULTI_MODEL_SPECIALIZED, Duration.ofSeconds(2), solver(openai),
                new ExecutionPlan.Assignment(gemini, ModelRole.SECURITY_REVIEWER),
                new ExecutionPlan.Assignment(claude, ModelRole.ALTERNATIVE_SOLVER)));
        assertThat(result.content()).isEqualTo("Final");
    }
    @Test void sequentialStageSeesPriorCritiqueAndStopsAtFourCalls() {
        when(providers.get("openai").generate(any())).thenReturn(new AiCompletion("Draft"), new AiCompletion("Final"));
        when(providers.get("gemini").generate(any())).thenReturn(new AiCompletion("Critique"));
        when(providers.get("anthropic").generate(any())).thenAnswer(call -> {
            AiChatRequest input = call.getArgument(0);
            assertThat(input.messages().getLast().content()).contains("Draft", "Critique");
            return new AiCompletion("Refined solution");
        });
        assertThat(run(plan(ExecutionStrategy.MULTI_MODEL_SEQUENTIAL, Duration.ofSeconds(1), solver(openai),
                new ExecutionPlan.Assignment(gemini, ModelRole.REVIEWER),
                new ExecutionPlan.Assignment(claude, ModelRole.ALTERNATIVE_SOLVER))).content()).isEqualTo("Final");
        verify(providers.get("openai"), times(2)).generate(any());
        verify(providers.get("gemini"), times(1)).generate(any());
        verify(providers.get("anthropic"), times(1)).generate(any());
    }
    @Test void allFailuresProduceNormalProviderFailure() {
        when(providers.get("openai").generate(any())).thenThrow(new AiProviderException("fail"));
        when(providers.get("gemini").generate(any())).thenThrow(new AiProviderException("fail"));
        assertThatThrownBy(() -> run(plan(ExecutionStrategy.MULTI_MODEL_PARALLEL, Duration.ofSeconds(1), solver(openai), solver(gemini))))
                .isInstanceOf(AiProviderException.class).hasMessage("No AI model could complete the request.");
    }
    @Test void cancellationInterruptsChildrenAndDoesNotStartSynthesis() throws Exception {
        var started = new CountDownLatch(2);
        var stopped = new CountDownLatch(2);
        for (String name : List.of("openai", "gemini")) when(providers.get(name).generate(any())).thenAnswer(call -> {
            started.countDown();
            try { new CountDownLatch(1).await(); }
            catch (InterruptedException cancelled) { stopped.countDown(); Thread.currentThread().interrupt(); }
            throw new AiProviderException("cancelled");
        });
        var cancelled = new AtomicBoolean();
        Thread caller = new Thread(() -> {
            try { run(plan(ExecutionStrategy.MULTI_MODEL_PARALLEL, Duration.ofSeconds(2), solver(openai), solver(gemini))); }
            catch (AiProviderException expected) { cancelled.set(Thread.currentThread().isInterrupted()); }
        });
        caller.start();
        try {
            assertThat(started.await(1, TimeUnit.SECONDS)).isTrue();
            caller.interrupt();
            caller.join(1000);
            assertThat(cancelled).isTrue();
            assertThat(stopped.await(1, TimeUnit.SECONDS)).isTrue();
            verify(providers.get("openai"), times(1)).generate(any());
        } finally { caller.interrupt(); caller.join(1000); }
    }
    @Test void overallDeadlinePreventsLaterStagesEvenWhenStageBudgetIsLonger() {
        when(providers.get("openai").generate(any())).thenAnswer(call -> {
            try { new CountDownLatch(1).await(); }
            catch (InterruptedException cancelled) { Thread.currentThread().interrupt(); }
            throw new AiProviderException("cancelled");
        });
        var bounded = new ExecutionPlan(ExecutionStrategy.MULTI_MODEL_REVIEW,
                List.of(solver(openai), new ExecutionPlan.Assignment(gemini, ModelRole.REVIEWER)), openai,
                Duration.ofSeconds(2), Duration.ofMillis(100), true);
        long start = System.nanoTime();
        assertThatThrownBy(() -> run(bounded)).isInstanceOf(AiProviderException.class);
        assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(1));
        verifyNoInteractions(providers.get("gemini"));
    }

    @Test void saturatedExecutorFailsPredictablyWithoutRunningOnCaller() {
        executor.shutdownNow();
        assertThatThrownBy(() -> run(plan(ExecutionStrategy.MULTI_MODEL_PARALLEL, Duration.ofMillis(100), solver(openai), solver(gemini))))
                .isInstanceOf(AiProviderException.class);
        providers.values().forEach(p -> verifyNoInteractions(p));
    }
}
