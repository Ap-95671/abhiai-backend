package com.abhiai.abhiai_backend.ai.orchestration;

import java.util.Set;

public record AiModelDefinition(
        String id,
        String provider,
        String providerModelId,
        String displayName,
        String description,
        long contextWindow,
        Set<ModelCapability> capabilities,
        double qualityScore,
        double speedScore,
        double costScore,
        ModelStatus status) {

    public AiModelDefinition {
        capabilities = Set.copyOf(capabilities);
    }
}
