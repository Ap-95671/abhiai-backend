package com.abhiai.abhiai_backend.ai.image;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai.image")
public class ImageGenerationRoutingProperties {

    private String provider = "cloudflare";
    private boolean geminiFallbackEnabled;

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public boolean isGeminiFallbackEnabled() { return geminiFallbackEnabled; }
    public void setGeminiFallbackEnabled(boolean geminiFallbackEnabled) {
        this.geminiFallbackEnabled = geminiFallbackEnabled;
    }
}
