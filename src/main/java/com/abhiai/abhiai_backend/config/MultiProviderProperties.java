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
