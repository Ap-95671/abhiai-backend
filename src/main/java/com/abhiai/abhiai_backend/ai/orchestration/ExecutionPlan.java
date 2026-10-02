package com.abhiai.abhiai_backend.ai.orchestration;

import java.time.Duration;
import java.util.List;

public record ExecutionPlan(ExecutionStrategy strategy, List<Assignment> assignments,
                            AiModelDefinition synthesisModel, Duration stageTimeout, Duration requestTimeout,
                            boolean fallbackAllowed, int maxRelativeCostUnits) {
    public static final int MAX_STAGES = 4;
    public ExecutionPlan(ExecutionStrategy strategy, List<Assignment> assignments, AiModelDefinition synthesisModel,
                         Duration stageTimeout, Duration requestTimeout, boolean fallbackAllowed) {
        this(strategy, assignments, synthesisModel, stageTimeout, requestTimeout, fallbackAllowed, 12);
    }
    public ExecutionPlan {
        assignments = List.copyOf(assignments);
        if (maxRelativeCostUnits < 1 || maxRelativeCostUnits > 12
                || assignments.stream().mapToInt(a -> a.model().costUnits()).sum() + (synthesisModel == null ? 0 : synthesisModel.costUnits()) > maxRelativeCostUnits)
            throw new IllegalArgumentException("Execution plan exceeds relative cost budget");
        if (stageTimeout == null || requestTimeout == null || stageTimeout.isNegative() || stageTimeout.isZero()
                || requestTimeout.isNegative() || requestTimeout.isZero() || requestTimeout.compareTo(Duration.ofSeconds(100)) > 0)
            throw new IllegalArgumentException("Execution plan requires bounded positive timeouts");
        if (strategy.name().startsWith("MULTI_MODEL_") && (assignments.size() < 2 || synthesisModel == null))
            throw new IllegalArgumentException("Multi-model execution requires solvers and a synthesis model");
        if (assignments.size() + (synthesisModel == null ? 0 : 1) > MAX_STAGES)
            throw new IllegalArgumentException("Execution plan exceeds stage limit");
    }
    public record Assignment(AiModelDefinition model, ModelRole role) { }
    public boolean multiModel() { return strategy.name().startsWith("MULTI_MODEL_"); }
}
