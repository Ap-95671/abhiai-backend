package com.abhiai.abhiai_backend.ai.groq;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.abhiai.abhiai_backend.exception.AiProviderFailureKind;

class GroqProviderTest {

    @Test
    void classifiesADecommissionedModelAsUnavailable() {
        var failure = GroqProvider.providerFailure(400, "{\"error\":{\"code\":\"model_decommissioned\"}}");

        assertThat(failure.kind()).isEqualTo(AiProviderFailureKind.MODEL_UNAVAILABLE);
        assertThat(failure).hasMessageContaining("GROQ_MODEL");
    }

    @Test
    void classifiesCapacityAndRateLimitsAsRetryableRateLimits() {
        assertThat(GroqProvider.providerFailure(429, "").kind()).isEqualTo(AiProviderFailureKind.RATE_LIMIT);
        assertThat(GroqProvider.providerFailure(498, "").kind()).isEqualTo(AiProviderFailureKind.RATE_LIMIT);
    }

    @Test
    void keepsAuthenticationAndAuthorizationFailuresDistinct() {
        assertThat(GroqProvider.providerFailure(401, "").kind()).isEqualTo(AiProviderFailureKind.AUTHENTICATION);
        assertThat(GroqProvider.providerFailure(403, "").kind()).isEqualTo(AiProviderFailureKind.AUTHORIZATION);
    }
}
