package com.abhiai.abhiai_backend.ai.ollama;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.net.http.HttpClient;

import org.junit.jupiter.api.Test;

import com.abhiai.abhiai_backend.exception.AiProviderUnavailableException;

import tools.jackson.databind.ObjectMapper;

class OllamaProviderTest {

    @Test
    void disabledProviderIsNotAdvertisedOrCalled() {
        OllamaProperties properties = new OllamaProperties();
        properties.setEnabled(false);
        OllamaProvider provider = new OllamaProvider(mock(HttpClient.class), new ObjectMapper(), properties);

        assertThat(provider.configured()).isFalse();
        assertThatThrownBy(() -> provider.generate(null))
                .isInstanceOf(AiProviderUnavailableException.class)
                .hasMessageContaining("disabled");
    }
}
