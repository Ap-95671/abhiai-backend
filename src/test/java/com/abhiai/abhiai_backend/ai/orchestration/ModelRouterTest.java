package com.abhiai.abhiai_backend.ai.orchestration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.abhiai.abhiai_backend.ai.AiChatMessage;
import com.abhiai.abhiai_backend.ai.AiChatRequest;
import com.abhiai.abhiai_backend.ai.ModelProvider;
import com.abhiai.abhiai_backend.entity.MessageRole;
import com.abhiai.abhiai_backend.exception.ModelRoutingException;

class ModelRouterTest {
    private ModelRegistry registry;
    private ModelRouter router;

    @BeforeEach void setUp() {
        registry = new ModelRegistry("gpt-test", "gemini-test", "groq-test", "local-test", "claude-test",
                "grok-test", "deepseek-test", "mistral-test", "cohere-test", "router-test");
        router = new ModelRouter(registry, new TaskClassifier(), new ProviderHealthTracker());
    }

    @Test void honorsConfiguredManualSelection() {
        ModelProvider provider = configured("openai");
        AiChatRequest request = new AiChatRequest(messages("hello"), List.of(), "MANUAL", "openai:gpt-test", false, null);
        assertThat(router.route(request, Map.of("openai", provider)).candidates()).extracting(AiModelDefinition::id)
                .containsExactly("openai:gpt-test");
    }

    @Test void keepsAbhenaVisibleButNotSelectable() {
        AiChatRequest request = new AiChatRequest(messages("hello"), List.of(), "MANUAL", "abhena:preview", false, null);
        assertThatThrownBy(() -> router.route(request, Map.of())).isInstanceOf(ModelRoutingException.class)
                .hasMessageContaining("coming soon");
    }

    @Test void autoExcludesProvidersWithoutCredentials() {
        var decision = router.route(new AiChatRequest(messages("write code")), Map.of("groq", configured("groq")));
        assertThat(decision.candidates()).allMatch(model -> model.provider().equals("groq"));
    }

    @Test void healthTrackerSurfacesRateLimitedProvidersAfterRepeatedFailures() {
        ProviderHealthTracker health = new ProviderHealthTracker();
        for (int index = 0; index < 3; index++) health.failure("gemini", new RuntimeException("HTTP 429 quota reached"));
        assertThat(health.status("gemini", true)).isEqualTo(ModelStatus.RATE_LIMITED);
    }

    private ModelProvider configured(String name) {
        ModelProvider provider = mock(ModelProvider.class);
        when(provider.providerName()).thenReturn(name);
        when(provider.configured()).thenReturn(true);
        return provider;
    }
    private List<AiChatMessage> messages(String text) { return List.of(new AiChatMessage(MessageRole.USER, text)); }
}
