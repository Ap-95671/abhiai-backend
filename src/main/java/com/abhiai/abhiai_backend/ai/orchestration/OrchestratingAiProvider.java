package com.abhiai.abhiai_backend.ai.orchestration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import com.abhiai.abhiai_backend.ai.AiChatRequest;
import com.abhiai.abhiai_backend.ai.AiCompletion;
import com.abhiai.abhiai_backend.ai.AiProvider;
import com.abhiai.abhiai_backend.ai.ModelProvider;
import com.abhiai.abhiai_backend.exception.AiProviderException;
import com.abhiai.abhiai_backend.exception.ModelRoutingException;

@Service
@Primary
public class OrchestratingAiProvider implements AiProvider {
    private static final Logger log = LoggerFactory.getLogger(OrchestratingAiProvider.class);
    private static final int MAX_ATTEMPTS = 3;
    private final Map<String, ModelProvider> providers;
    private final ModelRouter router;
    private RoutingMetrics metrics = new RoutingMetrics();
    private com.abhiai.abhiai_backend.ai.pipeline.ResponseQualityEvaluator evaluator =
            new com.abhiai.abhiai_backend.ai.pipeline.ResponseQualityEvaluator(new com.abhiai.abhiai_backend.config.AiOrchestrationProperties(), new tools.jackson.databind.ObjectMapper());
    @org.springframework.beans.factory.annotation.Autowired
    public void configureIntelligence(RoutingMetrics metrics, com.abhiai.abhiai_backend.ai.pipeline.ResponseQualityEvaluator evaluator) {
        this.metrics = metrics; this.evaluator = evaluator;
    }
    private final ProviderHealthTracker health;
    private final com.abhiai.abhiai_backend.ai.pipeline.AiRequestProcessor processor;
    private final ExecutionPlanner planner;
    private final MultiModelOrchestrator multiModel;
    private final com.abhiai.abhiai_backend.ai.pipeline.ResponseProcessor responses;

    public OrchestratingAiProvider(List<ModelProvider> providers, ModelRouter router, ProviderHealthTracker health,
            com.abhiai.abhiai_backend.ai.pipeline.AiRequestProcessor processor, ExecutionPlanner planner,
            MultiModelOrchestrator multiModel, com.abhiai.abhiai_backend.ai.pipeline.ResponseProcessor responses) {
        this.providers = new LinkedHashMap<>();
        providers.forEach(provider -> this.providers.put(provider.providerName(), provider));
        this.router = router;
        this.health = health;
        this.processor = processor;
        this.planner = planner;
        this.multiModel = multiModel;
        this.responses = responses;
    }

    @Override public String providerName() { return "abhiai-auto"; }
    @Override public String modelName() { return "auto"; }
    @Override public boolean configured() { return providers.values().stream().anyMatch(ModelProvider::configured); }
    @Override public boolean supportsImageUnderstanding() { return true; }

    @Override
    public AiCompletion generate(AiChatRequest request) {
        return execute(request, null);
    }

    @Override
    public AiCompletion generateStream(AiChatRequest request, Consumer<String> onTextChunk) {
        return execute(request, onTextChunk);
    }

    private AiCompletion execute(AiChatRequest request, Consumer<String> chunks) {
        long started = System.nanoTime();
        var normalized = processor.process(request);
        boolean success = false;
        try {
            AiCompletion completion = executePlan(normalized, chunks);
            success = true;
            return completion;
        } finally {
            log.info("ai_pipeline requestId={} intent={} complexity={} capability={} success={} latencyMs={}",
                    normalized.requestId(), normalized.intent(), normalized.analysis().complexity(), normalized.capability(),
                    success, (System.nanoTime() - started) / 1_000_000);
        }
    }

    private AiCompletion executePlan(com.abhiai.abhiai_backend.ai.pipeline.AiRequestProcessor.Request normalized, Consumer<String> chunks) {
        AiChatRequest request = normalized.input();
        String requestId = normalized.requestId();
        if (normalized.capability() == com.abhiai.abhiai_backend.ai.pipeline.Capability.IMAGE_GENERATION)
            throw new ModelRoutingException("IMAGE_PIPELINE_REQUIRED", "Image requests must use the conversation image pipeline.");
        RoutingDecision decision = router.route(request, normalized.analysis(), providers);
        ExecutionPlan plan = planner.plan(normalized, decision.candidates());
        log.info("ai_plan requestId={} strategy={} models={}", requestId, plan.strategy(),
                plan.assignments().stream().map(a -> a.model().id()).toList());
        if (plan.multiModel()) {
            AiCompletion completion = responses.process(multiModel.execute(normalized, plan, providers));
            if (chunks != null) chunks.accept(completion.content());
            return evaluated(normalized, plan, completion);
        }
        log.info("ai_routing requestId={} task={} complexity={} candidates={} reason={}", requestId,
                decision.classification().taskType(), decision.classification().complexity(),
                decision.candidates().stream().map(AiModelDefinition::id).toList(), decision.reason());
        AiProviderException lastFailure = null;
        int attempts = 0;
        int costUnits = 0;
        for (AiModelDefinition model : decision.candidates()) {
            if (Thread.currentThread().isInterrupted()) throw new AiProviderException("AI request was cancelled.");
            if (attempts >= MAX_ATTEMPTS) break;
            if (costUnits + model.costUnits() > plan.maxRelativeCostUnits()) continue;
            if (!health.tryAcquire(model.provider())) continue;
            attempts++;
            costUnits += model.costUnits();
            ModelProvider provider = providers.get(model.provider());
            long started = System.nanoTime();
            final boolean[] emitted = {false};
            try {
                AiChatRequest providerRequest = request.withProviderModelId(model.providerModelId());
                AiCompletion raw = chunks == null
                        ? provider.generate(providerRequest)
                        : provider.generateStream(providerRequest, chunk -> { emitted[0] = true; chunks.accept(chunk); });
                responses.process(raw);
                health.success(model.provider());
                long latency = (System.nanoTime() - started) / 1_000_000;
                log.info("ai_selected requestId={} provider={} model={} latencyMs={} fallback={}", requestId,
                        model.provider(), model.providerModelId(), latency, attempts > 1 || ("MANUAL".equals(request.selectionMode()) && !model.id().equals(request.selectedModelId())));
                var completion = raw.attributed(model.provider(), model.providerModelId(), latency, attempts > 1 || ("MANUAL".equals(request.selectionMode()) && !model.id().equals(request.selectedModelId())));
                metrics.record(new RoutingMetrics.Key(model.provider(), model.providerModelId(), normalized.intent(), normalized.analysis().taskType(), plan.strategy()),
                        true, latency, completion, completion.fallbackUsed());
                return evaluated(normalized, plan, completion);
            } catch (AiProviderException exception) {
                health.failure(model.provider(), exception);
                metrics.record(new RoutingMetrics.Key(model.provider(), model.providerModelId(), normalized.intent(), normalized.analysis().taskType(), plan.strategy()),
                        false, (System.nanoTime()-started)/1_000_000, null, attempts > 1);
                lastFailure = exception;
                log.warn("ai_provider_failure requestId={} provider={} model={} attempt={} kind={}", requestId,
                        model.provider(), model.providerModelId(), attempts, com.abhiai.abhiai_backend.exception.AiProviderFailureKind.classify(exception));
                if (emitted[0] || !request.fallbackAllowed() || Thread.currentThread().isInterrupted()
                        || exception.kind() == com.abhiai.abhiai_backend.exception.AiProviderFailureKind.CONTENT_RESTRICTION
                        || exception.kind() == com.abhiai.abhiai_backend.exception.AiProviderFailureKind.INVALID_REQUEST) throw exception;
            }
        }
        if (lastFailure != null) throw lastFailure;
        throw new ModelRoutingException("NO_MODEL_AVAILABLE", "No AI model could complete the request.");
    }
    private AiCompletion evaluated(com.abhiai.abhiai_backend.ai.pipeline.AiRequestProcessor.Request request, ExecutionPlan plan, AiCompletion completion) {
        var quality = evaluator.evaluate(request, plan, completion.content());
        if (quality != null) log.info("ai_quality requestId={} complete={} violations={} missing={} score={}",
                request.requestId(), quality.complete(), quality.constraintViolations(), quality.missingSections(), quality.qualityScore());
        log.info("ai_execution requestId={} conversationId={} strategy={} tools={} fallback={} inputTokens={} outputTokens={}",
                request.requestId(), request.input().executionContext().conversationId(), plan.strategy(),
                request.input().executionContext().toolCalls(), completion.fallbackUsed(), completion.inputTokens(), completion.outputTokens());
        return completion.withExecution(new com.abhiai.abhiai_backend.ai.pipeline.AiExecutionMetadata(request.requestId(), request.intent(),
                request.analysis().taskType(), request.analysis().complexity(), plan.strategy(), quality == null ? null : quality.qualityScore()));
    }

}
