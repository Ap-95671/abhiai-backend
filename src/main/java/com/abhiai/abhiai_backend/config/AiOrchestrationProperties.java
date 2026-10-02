package com.abhiai.abhiai_backend.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai.orchestration")
public class AiOrchestrationProperties {
    private boolean enabled = true;
    public enum ExecutionProfile { FAST, BALANCED, HIGH }
    private ExecutionProfile profile = ExecutionProfile.BALANCED;
    private int maxModels = 3;
    private int maxStages = 4;
    private int maxRelativeCostUnits = 12;
    private int circuitFailureThreshold = 3;
    private Duration circuitCooldown = Duration.ofSeconds(45);
    private Duration authenticationCooldown = Duration.ofMinutes(15);
    private boolean qualityEvaluationEnabled = true;
    private double adaptiveWeight = .20;
    public ExecutionProfile getProfile() { return profile; }
    public void setProfile(ExecutionProfile value) { profile = java.util.Objects.requireNonNull(value); }
    public int getMaxModels() { return maxModels; }
    public void setMaxModels(int value) { maxModels = limit(value, 1, 3); }
    public int getMaxStages() { return maxStages; }
    public void setMaxStages(int value) { maxStages = limit(value, 1, 4); }
    public int getMaxRelativeCostUnits() { return maxRelativeCostUnits; }
    public void setMaxRelativeCostUnits(int value) { maxRelativeCostUnits = limit(value, 1, 12); }
    public int getCircuitFailureThreshold() { return circuitFailureThreshold; }
    public void setCircuitFailureThreshold(int value) { circuitFailureThreshold = limit(value, 1, 20); }
    public Duration getCircuitCooldown() { return circuitCooldown; }
    public void setCircuitCooldown(Duration value) { circuitCooldown = positive(value); }
    public Duration getAuthenticationCooldown() { return authenticationCooldown; }
    public void setAuthenticationCooldown(Duration value) { authenticationCooldown = positive(value); }
    public boolean isQualityEvaluationEnabled() { return qualityEvaluationEnabled; }
    public void setQualityEvaluationEnabled(boolean value) { qualityEvaluationEnabled = value; }
    public double getAdaptiveWeight() { return adaptiveWeight; }
    public void setAdaptiveWeight(double value) {
        if (!Double.isFinite(value) || value < 0 || value > .4) throw new IllegalArgumentException("Adaptive weight must be between 0 and 0.4");
        adaptiveWeight = value;
    }
    public int costBudget() { return Math.min(maxRelativeCostUnits, profile == ExecutionProfile.FAST ? 3 : profile == ExecutionProfile.BALANCED ? 9 : 12); }
    private int limit(int value, int min, int max) {
        if (value < min || value > max) throw new IllegalArgumentException("AI budget is outside supported bounds");
        return value;
    }
    private Duration positive(Duration value) {
        if (value == null || value.isNegative() || value.isZero()) throw new IllegalArgumentException("Cooldown must be positive");
        return value;
    }
    private Duration stageTimeout = Duration.ofSeconds(20);
    private Duration requestTimeout = Duration.ofSeconds(90);
    private String synthesisModelId = "";
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean value) { enabled = value; }
    public Duration getStageTimeout() { return stageTimeout; }
    public void setStageTimeout(Duration value) { stageTimeout = bounded(value, 30); }
    public Duration getRequestTimeout() { return requestTimeout; }
    public void setRequestTimeout(Duration value) { requestTimeout = bounded(value, 100); }
    public String getSynthesisModelId() { return synthesisModelId; }
    public void setSynthesisModelId(String value) { synthesisModelId = value; }
    private Duration bounded(Duration value, int maximumSeconds) {
        if (value == null || value.isNegative() || value.isZero() || value.compareTo(Duration.ofSeconds(maximumSeconds)) > 0)
            throw new IllegalArgumentException("Orchestration timeout must be positive and within the hard request limit");
        return value;
    }
}
