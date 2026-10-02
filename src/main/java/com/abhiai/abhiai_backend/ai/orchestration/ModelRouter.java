package com.abhiai.abhiai_backend.ai.orchestration;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.abhiai.abhiai_backend.ai.AiChatRequest;
import com.abhiai.abhiai_backend.ai.ModelProvider;
import com.abhiai.abhiai_backend.exception.ModelRoutingException;

@Component
public class ModelRouter {
    private final ModelRegistry registry;
    private final TaskClassifier classifier;
    private final ProviderHealthTracker health;
    private RoutingMetrics metrics = new RoutingMetrics();
    private com.abhiai.abhiai_backend.config.AiOrchestrationProperties properties = new com.abhiai.abhiai_backend.config.AiOrchestrationProperties();
    @org.springframework.beans.factory.annotation.Autowired
    public void configureAdaptive(RoutingMetrics metrics, com.abhiai.abhiai_backend.config.AiOrchestrationProperties properties) {
        this.metrics = metrics; this.properties = properties;
    }

    public ModelRouter(ModelRegistry registry, TaskClassifier classifier, ProviderHealthTracker health) {
        this.registry = registry;
        this.classifier = classifier;
        this.health = health;
    }

    public RoutingDecision route(AiChatRequest request, Map<String, ModelProvider> providers) {
        return route(request, classifier.classify(request), providers);
    }

    public RoutingDecision route(AiChatRequest request, TaskClassification classification, Map<String, ModelProvider> providers) {
        // Conservative character estimate (including non-Latin text) plus output, image and orchestration reserve.
        long contextRequired = request.messages().stream().mapToLong(m -> m.content().length()).sum()
                + request.attachments().size() * 4096L + 20000;
        if (SelectionMode.from(request.selectionMode()) == SelectionMode.MANUAL) {
            AiModelDefinition selected = registry.find(request.selectedModelId())
                    .orElseThrow(() -> new ModelRoutingException("MODEL_NOT_FOUND", "The selected AI model does not exist."));
            try { validate(selected, classification, providers, contextRequired); }
            catch(ModelRoutingException unavailable) {
                if(!request.fallbackAllowed() || !unavailable.getCode().equals("MODEL_UNAVAILABLE"))throw unavailable;
                var alternatives=eligible(classification,providers, contextRequired).stream().filter(m->!m.id().equals(selected.id()))
                    .sorted(ranking(classification).reversed()).toList();
                if(alternatives.isEmpty())throw unavailable;
                return new RoutingDecision(classification,alternatives,"User-approved fallback because selected provider is unavailable");
            }
            List<AiModelDefinition> candidates = request.fallbackAllowed()
                    ? appendFallbacks(selected, classification, providers, contextRequired)
                    : List.of(selected);
            return new RoutingDecision(classification, candidates, "User selected " + selected.displayName());
        }
        List<AiModelDefinition> candidates = eligible(classification, providers, contextRequired).stream()
                .sorted(ranking(classification).reversed())
                .toList();
        if (candidates.isEmpty()) throw new ModelRoutingException("NO_MODEL_AVAILABLE", "No configured AI model can handle this request right now.");
        return new RoutingDecision(classification, candidates, "AbhiAI Auto selected the best available model for " + classification.taskType());
    }

    private List<AiModelDefinition> appendFallbacks(AiModelDefinition selected, TaskClassification classification,
                                                     Map<String, ModelProvider> providers, long contextRequired) {
        return java.util.stream.Stream.concat(
                java.util.stream.Stream.of(selected),
                eligible(classification, providers, contextRequired).stream()
                        .filter(model -> !model.id().equals(selected.id()))
                        .sorted(ranking(classification).reversed()))
                .toList();
    }

    private List<AiModelDefinition> eligible(TaskClassification classification, Map<String, ModelProvider> providers, long contextRequired) {
        return registry.all().stream()
                .filter(model -> model.status() == ModelStatus.AVAILABLE)
                .filter(model -> model.contextWindow() >= contextRequired)
                .filter(model -> model.costUnits() <= properties.costBudget())
                .filter(model -> model.capabilities().containsAll(classification.requiredCapabilities()))
                .filter(model -> {
                    ModelProvider provider = providers.get(model.provider());
                    return provider != null && provider.configured() && health.canAttempt(model.provider());
                }).toList();
    }

    private void validate(AiModelDefinition model, TaskClassification classification, Map<String, ModelProvider> providers, long contextRequired) {
        if (model.status() == ModelStatus.COMING_SOON)
            throw new ModelRoutingException("MODEL_COMING_SOON", model.displayName() + " is coming soon.");
        ModelProvider provider = providers.get(model.provider());
        if (model.status() != ModelStatus.AVAILABLE || provider == null || !provider.configured() || !health.canAttempt(model.provider()))
            throw new ModelRoutingException("MODEL_UNAVAILABLE", model.displayName() + " is not configured or is temporarily unavailable.");
        if (model.costUnits() > properties.costBudget())
            throw new ModelRoutingException("MODEL_BUDGET_EXCEEDED", "The selected model exceeds this request's execution budget.");
        if (model.contextWindow() < contextRequired)
            throw new ModelRoutingException("CONTEXT_TOO_LARGE", "The selected model cannot fit this request context.");
        if (!model.capabilities().containsAll(classification.requiredCapabilities()))
            throw new ModelRoutingException("CAPABILITY_MISMATCH", model.displayName() + " cannot handle the required capabilities for this request.");
    }

    private Comparator<AiModelDefinition> ranking(TaskClassification classification) {
        var scores = new java.util.HashMap<String, Double>();
        return Comparator.comparingDouble(model -> scores.computeIfAbsent(model.id(), ignored -> score(model, classification)));
    }

    private double score(AiModelDefinition model, TaskClassification classification) {
        boolean demanding = classification.complexity().ordinal() >= RequestComplexity.HIGH.ordinal();
        double qualityWeight = demanding ? .85 : .38;
        double speedWeight = demanding ? .10 : classification.complexity() == RequestComplexity.LOW ? .36 : .22;
        if (properties.getProfile() == com.abhiai.abhiai_backend.config.AiOrchestrationProperties.ExecutionProfile.FAST) {
            qualityWeight = .20; speedWeight = .55;
        }
        double costWeight = 1 - qualityWeight - speedWeight;
        double score = model.qualityScore() * qualityWeight + model.speedScore() * speedWeight + model.costScore() * costWeight;
        if (classification.taskType() == TaskType.CODE && model.capabilities().contains(ModelCapability.CODE)) score += .10;
        if (classification.taskType() == TaskType.REASONING && model.capabilities().contains(ModelCapability.REASONING)) score += .12;
        if (classification.taskType() == TaskType.VISION && model.capabilities().contains(ModelCapability.VISION)) score += .15;
        if (health.status(model.provider(), true) == ModelStatus.DEGRADED) score -= .05;
        return score + properties.getAdaptiveWeight() * metrics.adjustment(model, classification.taskType());
    }
}
