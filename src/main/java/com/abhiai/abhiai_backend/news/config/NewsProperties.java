package com.abhiai.abhiai_backend.news.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.news")
public class NewsProperties {

    private String provider = "newsdata";
    private String apiKey;
    private String baseUrl = "https://newsdata.io/api/1/latest";
    private Duration refreshInterval = Duration.ofMinutes(15);
    private Duration staleRetention = Duration.ofHours(6);
    private Duration requestTimeout = Duration.ofSeconds(12);
    private String defaultRegion = "global";
    private String defaultLanguage = "en";

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public Duration getRefreshInterval() { return refreshInterval; }
    public void setRefreshInterval(Duration refreshInterval) { this.refreshInterval = refreshInterval; }
    public Duration getStaleRetention() { return staleRetention; }
    public void setStaleRetention(Duration staleRetention) { this.staleRetention = staleRetention; }
    public Duration getRequestTimeout() { return requestTimeout; }
    public void setRequestTimeout(Duration requestTimeout) { this.requestTimeout = requestTimeout; }
    public String getDefaultRegion() { return defaultRegion; }
    public void setDefaultRegion(String defaultRegion) { this.defaultRegion = defaultRegion; }
    public String getDefaultLanguage() { return defaultLanguage; }
    public void setDefaultLanguage(String defaultLanguage) { this.defaultLanguage = defaultLanguage; }
}
