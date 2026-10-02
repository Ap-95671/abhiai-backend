package com.abhiai.abhiai_backend.ai.orchestration;

import java.util.List;
import com.abhiai.abhiai_backend.ai.*;
import com.abhiai.abhiai_backend.ai.pipeline.*;
import com.abhiai.abhiai_backend.config.*;
import com.abhiai.abhiai_backend.entity.MessageRole;
import com.abhiai.abhiai_backend.service.AiConversationContextBuilder;
import static org.mockito.Mockito.*;

final class PipelineTestSupport {
    static AiRequestProcessor processor() {
        return new AiRequestProcessor(new IntentClassifier(), new TaskClassifier(), new CapabilityRouter(),
                new AiConversationContextBuilder(new AiContextProperties()));
    }
    static AiChatRequest request(String text) { return new AiChatRequest(List.of(new AiChatMessage(MessageRole.USER, text))); }
    static ModelRegistry registry() {
        return new ModelRegistry("gpt-test", "gemini-test", "groq-test", "local-test", "claude-test",
                "grok-test", "deepseek-test", "mistral-test", "cohere-test", "router-test");
    }
    static ModelProvider provider(String name) {
        var provider = mock(ModelProvider.class);
        when(provider.providerName()).thenReturn(name);
        when(provider.configured()).thenReturn(true);
        return provider;
    }
}
