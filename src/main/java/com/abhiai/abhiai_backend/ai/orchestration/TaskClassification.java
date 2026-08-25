package com.abhiai.abhiai_backend.ai.orchestration;

import java.util.Set;

public record TaskClassification(TaskType taskType, RequestComplexity complexity, Set<ModelCapability> requiredCapabilities) {
    public TaskClassification {
        requiredCapabilities = Set.copyOf(requiredCapabilities);
    }
}
