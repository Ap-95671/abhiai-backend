package com.abhiai.abhiai_backend.ai.orchestration;

import java.util.List;

public record RoutingDecision(TaskClassification classification, List<AiModelDefinition> candidates, String reason) {
    public RoutingDecision { candidates = List.copyOf(candidates); }
}
