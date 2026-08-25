package com.abhiai.abhiai_backend.ai.orchestration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
    private final ProviderHealthTracker health;

    public OrchestratingAiProvider(List<ModelProvider> providers, ModelRouter router, ProviderHealthTracker health) {
        this.providers = new LinkedHashMap<>();
        providers.forEach(provider -> this.providers.put(provider.providerName(), provider));
        this.router = router;
        this.health = health;
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
        String requestId = UUID.randomUUID().toString();
        RoutingDecision decision = router.route(request, providers);
        log.info("ai_routing requestId={} task={} complexity={} candidates={} reason={}", requestId,
                decision.classification().taskType(), decision.classification().complexity(),
                decision.candidates().stream().map(AiModelDefinition::id).toList(), decision.reason());
        AiProviderException lastFailure = null;
        int attempts = 0;
        for (AiModelDefinition model : decision.candidates()) {
            if (++attempts > MAX_ATTEMPTS) break;
            ModelProvider provider = providers.get(model.provider());
            long started = System.nanoTime();
            final boolean[] emitted = {false};
            try {
                AiChatRequest providerRequest = request.withProviderModelId(model.providerModelId());
                AiCompletion raw = chunks == null
                        ? provider.generate(providerRequest)
                        : provider.generateStream(providerRequest, chunk -> { emitted[0] = true; chunks.accept(chunk); });
                health.success(model.provider());
                long latency = (System.nanoTime() - started) / 1_000_000;
                log.info("ai_selected requestId={} provider={} model={} latencyMs={} fallback={}", requestId,
                        model.provider(), model.providerModelId(), latency, attempts > 1);
                return raw.attributed(model.provider(), model.providerModelId(), latency, attempts > 1);
            } catch (AiProviderException exception) {
                health.failure(model.provider(), exception);
                lastFailure = exception;
                log.warn("ai_provider_failure requestId={} provider={} model={} attempt={} message={}", requestId,
                        model.provider(), model.providerModelId(), attempts, exception.getMessage());
                if (emitted[0] || !request.fallbackAllowed()) throw exception;
            }
        }
        if (lastFailure != null) throw lastFailure;
        throw new ModelRoutingException("NO_MODEL_AVAILABLE", "No AI model could complete the request.");
    }
}
