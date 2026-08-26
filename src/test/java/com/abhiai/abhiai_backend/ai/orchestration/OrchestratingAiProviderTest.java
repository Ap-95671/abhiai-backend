package com.abhiai.abhiai_backend.ai.orchestration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.abhiai.abhiai_backend.ai.AiChatMessage;
import com.abhiai.abhiai_backend.ai.AiChatRequest;
import com.abhiai.abhiai_backend.ai.AiCompletion;
import com.abhiai.abhiai_backend.ai.ModelProvider;
import com.abhiai.abhiai_backend.entity.MessageRole;
import com.abhiai.abhiai_backend.exception.AiProviderException;

class OrchestratingAiProviderTest {
    private ModelProvider openai;
    private ModelProvider gemini;
    private OrchestratingAiProvider orchestrator;

    @BeforeEach void setUp() {
        openai = provider("openai");
        gemini = provider("gemini");
        var registry = new ModelRegistry("gpt-test", "gemini-test", "groq-test", "local-test", "claude-test",
                "grok-test", "deepseek-test", "mistral-test", "cohere-test", "router-test");
        var health = new ProviderHealthTracker();
        orchestrator = new OrchestratingAiProvider(List.of(openai, gemini),
                new ModelRouter(registry, new TaskClassifier(), health), health);
    }

    @Test void fallsBackBeforeAnyStreamingContentIsEmitted() {
        when(gemini.generate(any())).thenThrow(new AiProviderException("temporary"));
        when(openai.generate(any())).thenReturn(new AiCompletion("fallback reply"));

        AiCompletion result = orchestrator.generate(request(true));

        assertThat(result.content()).isEqualTo("fallback reply");
        assertThat(result.provider()).isEqualTo("openai");
        assertThat(result.fallbackUsed()).isTrue();
    }

    @Test void neverFallsBackAfterAStreamHasEmittedText() {
        when(gemini.generateStream(any(), any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked") java.util.function.Consumer<String> chunks = invocation.getArgument(1);
            chunks.accept("partial");
            throw new AiProviderException("stream interrupted");
        });

        assertThatThrownBy(() -> orchestrator.generateStream(request(true), ignored -> {}))
                .isInstanceOf(AiProviderException.class).hasMessage("stream interrupted");
        verify(openai, never()).generateStream(any(), any());
    }

    @Test void manualSelectionDoesNotSilentlySwitchProviders() {
        when(openai.generate(any())).thenThrow(new AiProviderException("selected provider failed"));

        AiChatRequest manualRequest = new AiChatRequest(
                List.of(new AiChatMessage(MessageRole.USER, "hello")),
                List.of(), "MANUAL", "openai:gpt-test", false, null);

        assertThatThrownBy(() -> orchestrator.generate(manualRequest))
                .isInstanceOf(AiProviderException.class)
                .hasMessage("selected provider failed");
        verify(gemini, never()).generate(any());
    }

    private ModelProvider provider(String name) {
        ModelProvider provider = mock(ModelProvider.class);
        when(provider.providerName()).thenReturn(name);
        when(provider.configured()).thenReturn(true);
        return provider;
    }

    private AiChatRequest request(boolean fallback) {
        return new AiChatRequest(List.of(new AiChatMessage(MessageRole.USER, "hello")), List.of(), "AUTO", null, fallback, null);
    }
}
