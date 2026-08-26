package com.abhiai.abhiai_backend.ai.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.net.http.HttpClient;

import org.junit.jupiter.api.Test;

import com.abhiai.abhiai_backend.config.MultiProviderProperties;
import com.abhiai.abhiai_backend.exception.AiProviderFailureKind;

import tools.jackson.databind.ObjectMapper;

class OpenAiCompatibleModelProviderTest {

    private final OpenAiCompatibleModelProvider provider = new OpenAiCompatibleModelProvider(
            "test-provider", mock(HttpClient.class), new ObjectMapper(), new MultiProviderProperties.Provider());

    @Test
    void distinguishesAccountFailures() {
        assertThat(provider.providerFailure(401).kind()).isEqualTo(AiProviderFailureKind.AUTHENTICATION);
        assertThat(provider.providerFailure(402).kind()).isEqualTo(AiProviderFailureKind.BILLING);
        assertThat(provider.providerFailure(403).kind()).isEqualTo(AiProviderFailureKind.AUTHORIZATION);
    }

    @Test
    void distinguishesModelRateLimitAndUpstreamFailures() {
        assertThat(provider.providerFailure(404).kind()).isEqualTo(AiProviderFailureKind.MODEL_UNAVAILABLE);
        assertThat(provider.providerFailure(429).kind()).isEqualTo(AiProviderFailureKind.RATE_LIMIT);
        assertThat(provider.providerFailure(503).kind()).isEqualTo(AiProviderFailureKind.UPSTREAM_UNAVAILABLE);
    }
}
