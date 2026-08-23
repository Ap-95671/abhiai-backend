package com.abhiai.abhiai_backend.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotNull;

@Component
@Validated
@ConfigurationProperties(prefix = "app.social.stories")
public class StoryProperties {

    @NotNull
    private Duration lifetime = Duration.ofHours(24);

    public Duration getLifetime() {
        return lifetime;
    }

    public void setLifetime(Duration lifetime) {
        this.lifetime = lifetime;
    }
}
