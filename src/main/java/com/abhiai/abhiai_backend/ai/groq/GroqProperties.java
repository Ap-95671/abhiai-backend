package com.abhiai.abhiai_backend.ai.groq;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai.groq")
public class GroqProperties {

    private String apiKey;
    private String baseUrl = "https://api.groq.com/openai/v1";
    private String model = "llama-3.1-8b-instant";
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
