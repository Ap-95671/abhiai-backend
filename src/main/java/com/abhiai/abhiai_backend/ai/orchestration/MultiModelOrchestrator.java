package com.abhiai.abhiai_backend.ai.orchestration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import com.abhiai.abhiai_backend.ai.AiChatRequest;
import com.abhiai.abhiai_backend.ai.AiCompletion;
import com.abhiai.abhiai_backend.ai.ModelProvider;
import com.abhiai.abhiai_backend.ai.pipeline.AiRequestProcessor.Request;
import com.abhiai.abhiai_backend.ai.pipeline.ResponseProcessor;
import com.abhiai.abhiai_backend.exception.AiProviderException;
import com.abhiai.abhiai_backend.exception.AiProviderFailureKind;
import static com.abhiai.abhiai_backend.ai.orchestration.StageResult.Status.*;

@Service
public class MultiModelOrchestrator {
    private static final Logger log = LoggerFactory.getLogger(MultiModelOrchestrator.class);
    private final ExecutorService executor;
    private RoutingMetrics metrics = new RoutingMetrics();
    @org.springframework.beans.factory.annotation.Autowired
    public void configureMetrics(RoutingMetrics metrics) { this.metrics = metrics; }
    private final ProviderHealthTracker health;
    private final ResultAggregator aggregator;
    private final ResponseSynthesizer synthesizer;
    private final ResponseProcessor responses;
    private final Map<String, Semaphore> providerSlots = new ConcurrentHashMap<>();

    public MultiModelOrchestrator(@Qualifier("aiOrchestrationExecutor") ExecutorService executor,
            ProviderHealthTracker health, ResultAggregator aggregator, ResponseSynthesizer synthesizer, ResponseProcessor responses) {
        this.executor = executor;
        this.health = health;
        this.aggregator = aggregator;
        this.synthesizer = synthesizer;
        this.responses = responses;
    }

    public AiCompletion execute(Request request, ExecutionPlan plan, Map<String, ModelProvider> providers) {
        if (!plan.multiModel() || plan.assignments().isEmpty()) throw new IllegalArgumentException("Expected multi-model plan");
        long started = System.nanoTime();
        long deadline = started + plan.requestTimeout().toNanos();
        var pending = new ArrayList<Pending>();
        var results = new ArrayList<StageResult>();
        try {
            if (plan.strategy() == ExecutionStrategy.MULTI_MODEL_PARALLEL) {
                for (var assignment : plan.assignments())
                    pending.add(submit(assignment, request.input(), List.of(), plan, deadline, providers));
                for (var stage : pending) results.add(await(stage, request.requestId()));
            } else {
                var first = submit(plan.assignments().getFirst(), request.input(), List.of(), plan, deadline, providers);
                pending.add(first);
                results.add(await(first, request.requestId()));
                var remaining = plan.assignments().subList(1, plan.assignments().size());
                if (plan.strategy() == ExecutionStrategy.MULTI_MODEL_SPECIALIZED && results.getFirst().successful()) {
                    var reviews = new ArrayList<Pending>();
                    var evidence = aggregator.collect(results);
                    for (var assignment : remaining) {
                        var stage = submit(assignment, request.input(), evidence, plan, deadline, providers);
                        pending.add(stage);
                        reviews.add(stage);
                    }
                    for (var stage : reviews) results.add(await(stage, request.requestId()));
                } else {
                    for (var assignment : remaining) {
                        // A failed primary must not leave only criticism as the user's final answer.
                        boolean hasAnswer = results.stream().anyMatch(r -> r.successful() && r.assignment().role().completeAnswer());
                        var effective = hasAnswer ? assignment : new ExecutionPlan.Assignment(assignment.model(), ModelRole.PRIMARY_SOLVER);
                        var stage = submit(effective, request.input(), aggregator.forStage(results), plan, deadline, providers);
                        pending.add(stage);
                        results.add(await(stage, request.requestId()));
                    }
                }
            }
            checkCancellation();
            var evidence = aggregator.collect(results);
            AiCompletion chosen = results.stream().filter(r -> r.successful() && r.assignment().role().completeAnswer())
                    .map(StageResult::completion).findFirst().orElse(null);
            boolean degraded = results.stream().anyMatch(r -> !r.successful());
            if (evidence.size() > 1 && System.nanoTime() < deadline) {
                var stage = submit(new ExecutionPlan.Assignment(plan.synthesisModel(), ModelRole.SYNTHESIZER),
                        request.input(), aggregator.forStage(results), plan, deadline, providers);
                pending.add(stage);
                var synthesis = await(stage, request.requestId());
                results.add(synthesis);
                if (synthesis.successful()) chosen = synthesis.completion();
                else degraded = true;
            } else if (evidence.size() > 1) degraded = true;
            if (chosen == null || degraded && !plan.fallbackAllowed())
                throw new AiProviderException("No AI model could complete the request.", AiProviderFailureKind.UPSTREAM_UNAVAILABLE);
            checkCancellation();
            // Store aggregate known usage in the existing message columns; per-model usage stays in safe stage logs.
            Integer input = sumTokens(results, true);
            Integer output = sumTokens(results, false);
            return new AiCompletion(chosen.content(), chosen.provider(), chosen.model(), chosen.finishReason(), input, output,
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started), degraded);
        } finally {
            for (var result : results) if (result.status() != SKIPPED) metrics.record(
                    new RoutingMetrics.Key(result.assignment().model().provider(), result.assignment().model().providerModelId(),
                            request.intent(), request.analysis().taskType(), plan.strategy()), result.successful(), result.latencyMs(),
                    result.completion(), results.stream().anyMatch(r -> !r.successful()));
            pending.forEach(stage -> { if (stage.future() != null && !stage.future().isDone()) stage.future().cancel(true); });
            if (executor instanceof ThreadPoolExecutor pool) pool.purge();
            log.info("ai_orchestration requestId={} strategy={} stageCount={} latencyMs={}", request.requestId(), plan.strategy(),
                    pending.size(), TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started));
        }
    }

    private Pending submit(ExecutionPlan.Assignment assignment, AiChatRequest original,
                           List<ResultAggregator.Evidence> evidence, ExecutionPlan plan, long requestDeadline,
                           Map<String, ModelProvider> providers) {
        checkCancellation();
        long started = System.nanoTime();
        long deadline = Math.min(requestDeadline, started + plan.stageTimeout().toNanos());
        ModelProvider provider = providers.get(assignment.model().provider());
        if (started >= deadline || provider == null || !health.canAttempt(assignment.model().provider()))
            return new Pending(assignment, null, started, deadline);
        var input = synthesizer.prepare(original, assignment.role(), evidence).withProviderModelId(assignment.model().providerModelId());
        try {
            Future<StageResult> future = executor.submit(() -> {
                org.slf4j.MDC.put("requestId", original.executionContext().requestId());
                try {
                var slots = providerSlots.computeIfAbsent(assignment.model().provider(), ignored -> new Semaphore(2));
                if (System.nanoTime() >= deadline || !slots.tryAcquire())
                    return failed(assignment, SKIPPED, AiProviderFailureKind.UPSTREAM_UNAVAILABLE, started);
                try {
                    if (!health.tryAcquire(assignment.model().provider()))
                        return failed(assignment, SKIPPED, AiProviderFailureKind.UPSTREAM_UNAVAILABLE, started);
                    checkCancellation();
                    var raw = responses.process(provider.generate(input));
                    if (System.nanoTime() > deadline || Thread.currentThread().isInterrupted())
                        return failed(assignment, TIMEOUT, AiProviderFailureKind.TIMEOUT, started);
                    long latency = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
                    return new StageResult(assignment, SUCCESS,
                            raw.attributed(assignment.model().provider(), assignment.model().providerModelId(), latency, false), null, latency);
                } catch (RuntimeException failure) {
                    var kind = AiProviderFailureKind.classify(failure);
                    return failed(assignment, kind == AiProviderFailureKind.TIMEOUT ? TIMEOUT
                            : kind == AiProviderFailureKind.RATE_LIMIT ? RATE_LIMITED : FAILED, kind, started);
                } finally { slots.release(); }
                } finally { org.slf4j.MDC.remove("requestId"); }
            });
            return new Pending(assignment, future, started, deadline);
        } catch (RejectedExecutionException busy) {
            return new Pending(assignment, null, started, deadline);
        }
    }

    private StageResult await(Pending pending, String requestId) {
        StageResult result;
        try {
            if (pending.future() == null) result = failed(pending.assignment(), SKIPPED, AiProviderFailureKind.UPSTREAM_UNAVAILABLE, pending.started());
            else result = pending.future().get(Math.max(1, pending.deadline() - System.nanoTime()), TimeUnit.NANOSECONDS);
        } catch (TimeoutException timeout) {
            pending.future().cancel(true);
            result = failed(pending.assignment(), TIMEOUT, AiProviderFailureKind.TIMEOUT, pending.started());
        } catch (InterruptedException cancelled) {
            Thread.currentThread().interrupt();
            throw new AiProviderException("AI request was cancelled.", cancelled);
        } catch (ExecutionException | CancellationException failure) {
            result = failed(pending.assignment(), FAILED, AiProviderFailureKind.UNKNOWN, pending.started());
        }
        if (result.successful()) health.success(result.assignment().model().provider());
        else if (result.status() != SKIPPED) health.failure(result.assignment().model().provider(),
                new AiProviderException("AI stage failed", result.failure()));
        log.info("ai_stage requestId={} provider={} model={} role={} status={} error={} latencyMs={} inputTokens={} outputTokens={}",
                requestId, result.assignment().model().provider(), result.assignment().model().providerModelId(), result.assignment().role(),
                result.status(), result.failure(), result.latencyMs(), result.successful() ? result.completion().inputTokens() : null,
                result.successful() ? result.completion().outputTokens() : null);
        return result;
    }

    private StageResult failed(ExecutionPlan.Assignment assignment, StageResult.Status status, AiProviderFailureKind failure, long started) {
        return new StageResult(assignment, status, null, failure, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started));
    }
    private Integer sumTokens(List<StageResult> results, boolean input) {
        var known = results.stream().filter(StageResult::successful)
                .map(r -> input ? r.completion().inputTokens() : r.completion().outputTokens()).filter(java.util.Objects::nonNull).toList();
        return known.isEmpty() ? null : known.stream().mapToInt(Integer::intValue).sum();
    }
    private void checkCancellation() {
        if (Thread.currentThread().isInterrupted()) throw new AiProviderException("AI request was cancelled.");
    }
    private record Pending(ExecutionPlan.Assignment assignment, Future<StageResult> future, long started, long deadline) { }
}
