package com.abhiai.abhiai_backend.config;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai.providers")
public class MultiProviderProperties {
    private Map<String, Provider> configs = new LinkedHashMap<>();

    public Map<String, Provider> getConfigs() { return configs; }
    public void setConfigs(Map<String, Provider> configs) { this.configs = configs; }
    public Provider provider(String key) { return configs.getOrDefault(key, new Provider()); }

    public static class Provider {
        private boolean enabled = true;
        private java.util.Set<com.abhiai.abhiai_backend.ai.orchestration.ModelCapability> capabilities;
        private Long contextWindow;
        private Double qualityScore;
        private Double speedScore;
        private Double costScore;
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean value) { enabled = value; }
        public java.util.Set<com.abhiai.abhiai_backend.ai.orchestration.ModelCapability> getCapabilities() { return capabilities; }
        public void setCapabilities(java.util.Set<com.abhiai.abhiai_backend.ai.orchestration.ModelCapability> value) { capabilities = value == null ? null : java.util.Set.copyOf(value); }
        public Long getContextWindow() { return contextWindow; }
        public void setContextWindow(Long value) {
            if (value != null && value <= 0) throw new IllegalArgumentException("Model context must be positive");
            contextWindow = value;
        }
        public Double getQualityScore() { return qualityScore; }
        public void setQualityScore(Double value) { qualityScore = score(value); }
        public Double getSpeedScore() { return speedScore; }
        public void setSpeedScore(Double value) { speedScore = score(value); }
        public Double getCostScore() { return costScore; }
        public void setCostScore(Double value) { costScore = score(value); }
        private Double score(Double value) {
            if (value != null && (!Double.isFinite(value) || value < 0 || value > 1)) throw new IllegalArgumentException("Model scores must be between 0 and 1");
            return value;
        }
        private String apiKey;
        private String baseUrl;
        private String model;
        private String instructions = "You are AbhiAI, a helpful, accurate, and concise AI assistant.";
        private Duration requestTimeout = Duration.ofSeconds(60);
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public String getInstructions() { return instructions; }
        public void setInstructions(String instructions) { this.instructions = instructions; }
        public Duration getRequestTimeout() { return requestTimeout; }
        public void setRequestTimeout(Duration requestTimeout) { this.requestTimeout = requestTimeout; }
    }
}
