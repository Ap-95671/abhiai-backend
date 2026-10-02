package com.abhiai.abhiai_backend.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai.web-search")
public class WebSearchProperties {
    private boolean enabled;
    private boolean autoEnabled = true;
    private int maxResults = 5;
    public boolean isAutoEnabled() { return autoEnabled; }
    public void setAutoEnabled(boolean value) { autoEnabled = value; }
    public int getMaxResults() { return maxResults; }
    public void setMaxResults(int value) {
        if (value < 1 || value > 8) throw new IllegalArgumentException("Search result limit must be 1..8");
        maxResults = value;
    }
    private String apiKey = "";
    private String baseUrl = "https://api.search.brave.com/res/v1/web/search";
    private Duration requestTimeout = Duration.ofSeconds(10);

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public Duration getRequestTimeout() { return requestTimeout; }
    public void setRequestTimeout(Duration requestTimeout) {
        if (requestTimeout == null || requestTimeout.isNegative() || requestTimeout.isZero() || requestTimeout.compareTo(Duration.ofSeconds(15)) > 0)
            throw new IllegalArgumentException("Search timeout must be positive and at most 15 seconds");
        this.requestTimeout = requestTimeout;
    }
}
