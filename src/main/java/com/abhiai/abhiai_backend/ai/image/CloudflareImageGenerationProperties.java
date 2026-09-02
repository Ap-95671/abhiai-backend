package com.abhiai.abhiai_backend.ai.image;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai.image.cloudflare")
public class CloudflareImageGenerationProperties {

    private String accountId = "";
    private String apiToken = "";
    private String baseUrl = "https://api.cloudflare.com/client/v4";
    private String model = "@cf/black-forest-labs/flux-1-schnell";
    private int steps = 4;
    private Duration requestTimeout = Duration.ofSeconds(120);

    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }
    public String getApiToken() { return apiToken; }
    public void setApiToken(String apiToken) { this.apiToken = apiToken; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public int getSteps() { return steps; }
    public void setSteps(int steps) { this.steps = steps; }
    public Duration getRequestTimeout() { return requestTimeout; }
    public void setRequestTimeout(Duration requestTimeout) { this.requestTimeout = requestTimeout; }
}
