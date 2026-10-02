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

    public enum Strength { UNSUPPORTED, STANDARD, HIGH }
    public enum CostClass { LOW, MEDIUM, HIGH }
    public Strength reasoningStrength() { return strength(ModelCapability.REASONING); }
    public Strength codingStrength() { return strength(ModelCapability.CODE); }
    public Strength documentStrength() { return strength(ModelCapability.TEXT); }
    private Strength strength(ModelCapability capability) {
        return !capabilities.contains(capability) ? Strength.UNSUPPORTED : qualityScore >= .85 ? Strength.HIGH : Strength.STANDARD;
    }
    public CostClass relativeCost() { return costScore >= .8 ? CostClass.LOW : costScore >= .6 ? CostClass.MEDIUM : CostClass.HIGH; }
    public int costUnits() { return relativeCost().ordinal() + 1; }
    public boolean supportsVision() { return capabilities.contains(ModelCapability.VISION); }
    public boolean supportsStreaming() { return capabilities.contains(ModelCapability.STREAMING); }

    public AiModelDefinition {
        capabilities = Set.copyOf(capabilities);
    }
}
