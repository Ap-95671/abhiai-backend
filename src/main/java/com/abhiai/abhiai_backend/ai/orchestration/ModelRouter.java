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

    public ModelRouter(ModelRegistry registry, TaskClassifier classifier, ProviderHealthTracker health) {
        this.registry = registry;
        this.classifier = classifier;
        this.health = health;
    }

    public RoutingDecision route(AiChatRequest request, Map<String, ModelProvider> providers) {
        TaskClassification classification = classifier.classify(request);
        if (SelectionMode.from(request.selectionMode()) == SelectionMode.MANUAL) {
            AiModelDefinition selected = registry.find(request.selectedModelId())
                    .orElseThrow(() -> new ModelRoutingException("MODEL_NOT_FOUND", "The selected AI model does not exist."));
            validate(selected, classification, providers);
            List<AiModelDefinition> candidates = request.fallbackAllowed()
                    ? appendFallbacks(selected, classification, providers)
                    : List.of(selected);
            return new RoutingDecision(classification, candidates, "User selected " + selected.displayName());
        }
        List<AiModelDefinition> candidates = eligible(classification, providers).stream()
                .sorted(Comparator.comparingDouble((AiModelDefinition model) -> score(model, classification)).reversed())
                .toList();
        if (candidates.isEmpty()) throw new ModelRoutingException("NO_MODEL_AVAILABLE", "No configured AI model can handle this request right now.");
        return new RoutingDecision(classification, candidates, "AbhiAI Auto selected the best available model for " + classification.taskType());
    }

    private List<AiModelDefinition> appendFallbacks(AiModelDefinition selected, TaskClassification classification,
                                                     Map<String, ModelProvider> providers) {
        return java.util.stream.Stream.concat(
                java.util.stream.Stream.of(selected),
                eligible(classification, providers).stream()
                        .filter(model -> !model.id().equals(selected.id()))
                        .sorted(Comparator.comparingDouble((AiModelDefinition model) -> score(model, classification)).reversed()))
                .toList();
    }

    private List<AiModelDefinition> eligible(TaskClassification classification, Map<String, ModelProvider> providers) {
        return registry.all().stream()
                .filter(model -> model.status() == ModelStatus.AVAILABLE)
                .filter(model -> model.capabilities().containsAll(classification.requiredCapabilities()))
                .filter(model -> {
                    ModelProvider provider = providers.get(model.provider());
                    return provider != null && provider.configured() && health.canAttempt(model.provider());
                }).toList();
    }

    private void validate(AiModelDefinition model, TaskClassification classification, Map<String, ModelProvider> providers) {
        if (model.status() == ModelStatus.COMING_SOON)
            throw new ModelRoutingException("MODEL_COMING_SOON", model.displayName() + " is coming soon.");
        ModelProvider provider = providers.get(model.provider());
        if (provider == null || !provider.configured() || !health.canAttempt(model.provider()))
            throw new ModelRoutingException("MODEL_UNAVAILABLE", model.displayName() + " is not configured or is temporarily unavailable.");
        if (!model.capabilities().containsAll(classification.requiredCapabilities()))
            throw new ModelRoutingException("CAPABILITY_MISMATCH", model.displayName() + " cannot handle the required capabilities for this request.");
    }

    private double score(AiModelDefinition model, TaskClassification classification) {
        double qualityWeight = classification.complexity() == RequestComplexity.HIGH ? .55 : .38;
        double speedWeight = classification.complexity() == RequestComplexity.LOW ? .36 : .22;
        double costWeight = 1 - qualityWeight - speedWeight;
        double score = model.qualityScore() * qualityWeight + model.speedScore() * speedWeight + model.costScore() * costWeight;
        if (classification.taskType() == TaskType.CODE && model.capabilities().contains(ModelCapability.CODE)) score += .10;
        if (classification.taskType() == TaskType.REASONING && model.capabilities().contains(ModelCapability.REASONING)) score += .12;
        if (classification.taskType() == TaskType.VISION && model.capabilities().contains(ModelCapability.VISION)) score += .15;
        return score;
    }
}
