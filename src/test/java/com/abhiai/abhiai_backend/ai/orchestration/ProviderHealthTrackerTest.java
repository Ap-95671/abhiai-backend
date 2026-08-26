package com.abhiai.abhiai_backend.ai.orchestration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.abhiai.abhiai_backend.exception.AiProviderException;
import com.abhiai.abhiai_backend.exception.AiProviderFailureKind;

class ProviderHealthTrackerTest {

    @Test
    void authenticationFailureDisablesProviderImmediately() {
        ProviderHealthTracker health = new ProviderHealthTracker();

        health.failure("xai", new AiProviderException("invalid key", AiProviderFailureKind.AUTHENTICATION));

        assertThat(health.canAttempt("xai")).isFalse();
        assertThat(health.status("xai", true)).isEqualTo(ModelStatus.UNAVAILABLE);
    }

    @Test
    void billingFailureDisablesProviderImmediately() {
        ProviderHealthTracker health = new ProviderHealthTracker();

        health.failure("deepseek", new AiProviderException("payment required", AiProviderFailureKind.BILLING));

        assertThat(health.canAttempt("deepseek")).isFalse();
        assertThat(health.status("deepseek", true)).isEqualTo(ModelStatus.UNAVAILABLE);
    }

    @Test
    void oneRateLimitFailureEntersCooldownImmediately() {
        ProviderHealthTracker health = new ProviderHealthTracker();

        health.failure("groq", new AiProviderException("rate limit", AiProviderFailureKind.RATE_LIMIT));

        assertThat(health.canAttempt("groq")).isFalse();
        assertThat(health.status("groq", true)).isEqualTo(ModelStatus.RATE_LIMITED);
    }

    @Test
    void oneTransientFailureOnlyDegradesProvider() {
        ProviderHealthTracker health = new ProviderHealthTracker();

        health.failure("gemini", new AiProviderException("temporary", AiProviderFailureKind.UPSTREAM_UNAVAILABLE));

        assertThat(health.canAttempt("gemini")).isTrue();
        assertThat(health.status("gemini", true)).isEqualTo(ModelStatus.DEGRADED);
    }
}
