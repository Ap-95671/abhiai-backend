package com.abhiai.abhiai_backend.ai.ollama;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai.ollama")
public class OllamaProperties {

    private String baseUrl = "http://host.docker.internal:11434";
    private String model = "gemma3:4b";
    private String instructions = "You are AbhiAI, a helpful, accurate, and concise AI assistant.";
    private Duration requestTimeout = Duration.ofSeconds(120);

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }
    public Duration getRequestTimeout() { return requestTimeout; }
    public void setRequestTimeout(Duration requestTimeout) { this.requestTimeout = requestTimeout; }
}
