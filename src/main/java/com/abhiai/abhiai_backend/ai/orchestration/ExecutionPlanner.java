package com.abhiai.abhiai_backend.ai.orchestration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import org.springframework.stereotype.Component;
import com.abhiai.abhiai_backend.ai.pipeline.AiRequestProcessor.Request;
import com.abhiai.abhiai_backend.ai.pipeline.Capability;
import com.abhiai.abhiai_backend.config.AiOrchestrationProperties;
import static com.abhiai.abhiai_backend.ai.pipeline.IntentClassifier.has;

@Component
public class ExecutionPlanner {
    private final AiOrchestrationProperties properties;
    private final com.abhiai.abhiai_backend.ai.pipeline.IntentClassifier classifier;
    public ExecutionPlanner(AiOrchestrationProperties properties) { this(properties, new com.abhiai.abhiai_backend.ai.pipeline.IntentClassifier()); }
    @org.springframework.beans.factory.annotation.Autowired
    public ExecutionPlanner(AiOrchestrationProperties properties, com.abhiai.abhiai_backend.ai.pipeline.IntentClassifier classifier) {
        this.properties = properties; this.classifier = classifier;
    }

    public record ToolPlan(boolean search, String reason) { }
    public ToolPlan planTools(String message, boolean manualSearch) {
        if (classifier.classify(message) == com.abhiai.abhiai_backend.ai.pipeline.Intent.IMAGE_GENERATION)
            return new ToolPlan(false, "image_pipeline");
        return new ToolPlan(manualSearch || classifier.requiresFreshInformation(message), manualSearch ? "manual" : "freshness");
    }

    public ExecutionPlan plan(Request request, List<AiModelDefinition> candidates) {
        if (request.capability() == Capability.IMAGE_GENERATION)
            return plan(ExecutionStrategy.SPECIALIZED_PIPELINE, List.of(), null, false);
        if (candidates.isEmpty()) throw new IllegalArgumentException("Text execution requires routed candidates");
        var distinct = new LinkedHashMap<String, AiModelDefinition>();
        candidates.forEach(model -> distinct.putIfAbsent(model.provider(), model));
        int budgetModels = Math.min(properties.getMaxModels(), properties.getMaxStages() - 1);
        if (properties.getProfile() == AiOrchestrationProperties.ExecutionProfile.BALANCED
                || request.analysis().complexity() == RequestComplexity.HIGH) budgetModels = Math.min(2, budgetModels);
        var models = distinct.values().stream().limit(Math.max(1, budgetModels)).toList();
        var primary = new ExecutionPlan.Assignment(models.getFirst(), ModelRole.PRIMARY_SOLVER);
        var complexity = request.analysis().complexity();
        if (!properties.isEnabled() || properties.getProfile() == AiOrchestrationProperties.ExecutionProfile.FAST || SelectionMode.from(request.input().selectionMode()) == SelectionMode.MANUAL
                || !request.input().fallbackAllowed() || models.size() < 2
                || complexity.ordinal() < RequestComplexity.HIGH.ordinal())
            return plan(ExecutionStrategy.SINGLE_MODEL, List.of(primary), null, request.input().fallbackAllowed());

        String text = request.input().originalMessage();
        var assignments = new ArrayList<ExecutionPlan.Assignment>();
        assignments.add(primary);
        ExecutionStrategy strategy;
        if (has(text, "draft") && has(text, "critique|review") && has(text, "refine|improve|revise")) {
            strategy = ExecutionStrategy.MULTI_MODEL_SEQUENTIAL;
            assignments.add(new ExecutionPlan.Assignment(models.get(1), ModelRole.REVIEWER));
            if (models.size() > 2) assignments.add(new ExecutionPlan.Assignment(models.get(2), ModelRole.ALTERNATIVE_SOLVER));
        } else if (complexity == RequestComplexity.VERY_HIGH && has(text, "security|privacy") && models.size() > 2) {
            strategy = ExecutionStrategy.MULTI_MODEL_SPECIALIZED;
            assignments.add(new ExecutionPlan.Assignment(models.get(1), ModelRole.SECURITY_REVIEWER));
            assignments.add(new ExecutionPlan.Assignment(models.get(2), ModelRole.ALTERNATIVE_SOLVER));
        } else if (complexity == RequestComplexity.VERY_HIGH || has(text, "compare") && has(text, "architecture|approaches|designs")) {
            strategy = ExecutionStrategy.MULTI_MODEL_PARALLEL;
            models.stream().skip(1).forEach(m -> assignments.add(new ExecutionPlan.Assignment(m, ModelRole.PRIMARY_SOLVER)));
        } else if (has(text, "review|security|correctness|bugs|critique|concurrency")) {
            strategy = ExecutionStrategy.MULTI_MODEL_REVIEW;
            assignments.add(new ExecutionPlan.Assignment(models.get(1), ModelRole.REVIEWER));
        } else return plan(ExecutionStrategy.SINGLE_MODEL, List.of(primary), null, request.input().fallbackAllowed());

        // The configured synthesizer must be one of the same router-approved eligible models.
        AiModelDefinition synthesis = candidates.stream().filter(m -> m.id().equals(properties.getSynthesisModelId()))
                .findFirst().orElse(models.getFirst());
        int cost = assignments.stream().mapToInt(a -> a.model().costUnits()).sum() + synthesis.costUnits();
        if (cost > properties.costBudget())
            return plan(ExecutionStrategy.SINGLE_MODEL, List.of(primary), null, request.input().fallbackAllowed());
        return plan(strategy, assignments, synthesis, request.input().fallbackAllowed());
    }

    private ExecutionPlan plan(ExecutionStrategy strategy, List<ExecutionPlan.Assignment> assignments,
                               AiModelDefinition synthesis, boolean fallback) {
        return new ExecutionPlan(strategy, assignments, synthesis, properties.getStageTimeout(), properties.getRequestTimeout(), fallback, properties.costBudget());
    }
}
