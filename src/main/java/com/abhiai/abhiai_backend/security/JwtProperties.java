package com.abhiai.abhiai_backend.security;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Validated
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    @NotBlank(message = "JWT secret must be configured")
    @Size(min = 32, message = "JWT secret must be at least 32 characters")
    private String secret;

    @NotNull(message = "JWT access-token TTL must be configured")
    private Duration accessTokenTtl = Duration.ofMinutes(15);

    private Duration refreshTokenTtl = Duration.ofDays(30);
    private Duration sessionTokenTtl = Duration.ofDays(1);
    public Duration getRefreshTokenTtl() { return refreshTokenTtl; }
    public void setRefreshTokenTtl(Duration value) { refreshTokenTtl = positive(value); }
    public Duration getSessionTokenTtl() { return sessionTokenTtl; }
    public void setSessionTokenTtl(Duration value) { sessionTokenTtl = positive(value); }
    private Duration positive(Duration value) {
        if (value == null || value.isNegative() || value.isZero()) throw new IllegalArgumentException("Session TTL must be positive");
        return value;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public Duration getAccessTokenTtl() {
        return accessTokenTtl;
    }

    public void setAccessTokenTtl(Duration accessTokenTtl) {
        this.accessTokenTtl = accessTokenTtl;
    }
}
