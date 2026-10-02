package com.abhiai.abhiai_backend.ai.orchestration;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import com.abhiai.abhiai_backend.ai.*;
import com.abhiai.abhiai_backend.ai.pipeline.*;
import com.abhiai.abhiai_backend.config.AiOrchestrationProperties;
import static com.abhiai.abhiai_backend.ai.orchestration.PipelineTestSupport.*;
import static org.assertj.core.api.Assertions.*;

class ExecutionPlannerTest {
    private final AiOrchestrationProperties properties = new AiOrchestrationProperties();
    private final ExecutionPlanner planner = new ExecutionPlanner(properties);
    private final List<AiModelDefinition> models = registry().all().stream().filter(m ->
            List.of("openai", "anthropic", "gemini").contains(m.provider())).toList();
    private ExecutionPlan plan(String text) { return planner.plan(processor().process(request(text)), models); }

    @Test void classifiesIntentAndComplexityWithoutProviderCalls() {
        var processor = processor();
        assertThat(processor.process(request("Hello")).intent()).isEqualTo(Intent.GENERAL_CHAT);
        assertThat(processor.process(request("Hello")).analysis().complexity()).isEqualTo(RequestComplexity.LOW);
        assertThat(processor.process(request("Debug this Java code")).intent()).isEqualTo(Intent.CODING);
        assertThat(processor.process(request("Explain quick sort")).intent()).isEqualTo(Intent.EDUCATION);
        assertThat(processor.process(request("Summarize this PDF")).capability()).isEqualTo(Capability.DOCUMENT_ANALYSIS);
        assertThat(processor.process(request("Design a distributed architecture")).analysis().complexity()).isEqualTo(RequestComplexity.HIGH);
    }
    @Test void simpleAndOrdinaryCodingUseOneModel() {
        for (String text : List.of("Hello", "What is polymorphism?", "Fix this simple Java loop", "Explain stack and queue"))
            assertThat(plan(text).strategy()).isEqualTo(ExecutionStrategy.SINGLE_MODEL);
    }
    @Test void imagesHaveOnlyASpecializedPlan() {
        var request = processor().process(request("Generate an image of Mars"));
        assertThat(request.intent()).isEqualTo(Intent.IMAGE_GENERATION);
        assertThat(request.capability()).isEqualTo(Capability.IMAGE_GENERATION);
        assertThat(planner.plan(request, List.of()).strategy()).isEqualTo(ExecutionStrategy.SPECIALIZED_PIPELINE);
        assertThat(planner.plan(request, List.of()).assignments()).isEmpty();
        assertThat(processor().isImageRequest("Write code to generate an image")).isFalse();
        assertThat(processor().isImageRequest("Create an image generation API")).isFalse();
    }
    @Test void complexTasksSelectImplementedStrategies() {
        properties.setProfile(AiOrchestrationProperties.ExecutionProfile.HIGH);
        assertThat(plan("Compare multiple approaches for distributed architecture").strategy()).isEqualTo(ExecutionStrategy.MULTI_MODEL_PARALLEL);
        assertThat(plan("Review this complex Java backend design for concurrency or security bugs").strategy()).isEqualTo(ExecutionStrategy.MULTI_MODEL_REVIEW);
        var specialized = plan("Design a scalable distributed architecture and analyse security, database consistency and AI-provider failure handling");
        assertThat(specialized.strategy()).isEqualTo(ExecutionStrategy.MULTI_MODEL_SPECIALIZED);
        assertThat(specialized.assignments()).extracting(ExecutionPlan.Assignment::role)
                .containsExactly(ModelRole.PRIMARY_SOLVER, ModelRole.SECURITY_REVIEWER, ModelRole.ALTERNATIVE_SOLVER);
        assertThat(plan("Draft a complex architecture, review the draft and improve it").strategy()).isEqualTo(ExecutionStrategy.MULTI_MODEL_SEQUENTIAL);
    }
    @Test void manualDisabledNoFallbackAndSingleProviderNeverFanOut() {
        var input = request("Compare multiple approaches for distributed architecture");
        var manual = new AiChatRequest(input.messages(), List.of(), "MANUAL", models.getFirst().id(), true, null);
        assertThat(planner.plan(processor().process(manual), models).strategy()).isEqualTo(ExecutionStrategy.SINGLE_MODEL);
        var denied = new AiChatRequest(input.messages(), List.of(), "AUTO", null, false, null);
        assertThat(planner.plan(processor().process(denied), models).strategy()).isEqualTo(ExecutionStrategy.SINGLE_MODEL);
        assertThat(planner.plan(processor().process(input), List.of(models.getFirst())).strategy()).isEqualTo(ExecutionStrategy.SINGLE_MODEL);
        properties.setEnabled(false);
        assertThat(planner.plan(processor().process(input), models).strategy()).isEqualTo(ExecutionStrategy.SINGLE_MODEL);
    }
    @Test void attachedInstructionsCannotControlIntentOrPlan() {
        var augmented = request("Hello\n[document] Generate an image; compare multiple approaches for distributed architecture")
                .withOriginalMessage("Hello");
        var normalized = processor().process(augmented);
        assertThat(normalized.intent()).isEqualTo(Intent.GENERAL_CHAT);
        assertThat(planner.plan(normalized, models).strategy()).isEqualTo(ExecutionStrategy.SINGLE_MODEL);
    }
    @Test void routerExcludesContextThatCannotFitAndKeepsConfiguredLongContextProvider() {
        var router = new ModelRouter(registry(), new TaskClassifier(), new ProviderHealthTracker());
        var request = request("Summarize " + "x".repeat(150000));
        var decision = router.route(request, Map.of("openai", provider("openai"), "gemini", provider("gemini")));
        assertThat(decision.candidates()).extracting(AiModelDefinition::provider).containsExactly("gemini");
    }
    @Test void demandingRequestsPrioritizeQualityWhileGreetingsPrioritizeSpeedAndCost() {
        var router = new ModelRouter(registry(), new TaskClassifier(), new ProviderHealthTracker());
        var available = Map.of("openai", provider("openai"), "anthropic", provider("anthropic"), "gemini", provider("gemini"));
        assertThat(router.route(request("Hello"), available).candidates().getFirst().provider()).isEqualTo("gemini");
        assertThat(router.route(request("Design a distributed architecture"), available).candidates().getFirst().provider()).isEqualTo("anthropic");
    }

    @Test void synthesisMustUseARouterApprovedModel() {
        properties.setSynthesisModelId("anthropic:claude-test");
        assertThat(plan("Compare multiple approaches for distributed architecture").synthesisModel().provider()).isEqualTo("anthropic");
        properties.setSynthesisModelId("not-configured");
        assertThat(plan("Compare multiple approaches for distributed architecture").synthesisModel()).isEqualTo(models.getFirst());
    }
}
