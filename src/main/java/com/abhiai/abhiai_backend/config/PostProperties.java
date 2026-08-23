package com.abhiai.abhiai_backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;

@Component
@Validated
@ConfigurationProperties(prefix = "app.social.posts")
public class PostProperties {

    @Min(value = 1, message = "Post text limit must be positive")
    private int maxTextLength = 1000;

    public int getMaxTextLength() {
        return maxTextLength;
    }

    public void setMaxTextLength(int maxTextLength) {
        this.maxTextLength = maxTextLength;
    }
}
