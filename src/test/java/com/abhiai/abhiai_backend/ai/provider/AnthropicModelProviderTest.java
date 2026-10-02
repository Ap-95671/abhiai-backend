package com.abhiai.abhiai_backend.ai.provider;

import java.net.http.*;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.Flow;
import java.io.ByteArrayOutputStream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import com.abhiai.abhiai_backend.ai.*;
import com.abhiai.abhiai_backend.config.MultiProviderProperties;
import com.abhiai.abhiai_backend.entity.MessageRole;
import com.abhiai.abhiai_backend.exception.*;
import tools.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AnthropicModelProviderTest {
    @SuppressWarnings("unchecked")
    @Test void preservesSystemPolicyVisionAndUsageInExistingAdapter() throws Exception {
        var client = mock(HttpClient.class);
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn("{\"model\":\"claude-test\",\"content\":[{\"type\":\"text\",\"text\":\"Answer\"}],\"usage\":{\"input_tokens\":10,\"output_tokens\":3}}");
        when(client.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        var properties = new MultiProviderProperties.Provider();
        properties.setApiKey("test-key"); properties.setBaseUrl("https://api.anthropic.com/v1"); properties.setModel("claude-test");
        var provider = new AnthropicModelProvider(client, new ObjectMapper(), properties);
        var request = new AiChatRequest(List.of(new AiChatMessage(MessageRole.SYSTEM, "Trusted stage policy"),
                new AiChatMessage(MessageRole.USER, "Describe the picture")),
                List.of(new AiInputAttachment("image.png", "image/png", new byte[]{1, 2})));
        var completion = provider.generate(request);
        assertThat(completion.content()).isEqualTo("Answer");
        assertThat(completion.inputTokens()).isEqualTo(10);
        var sent = ArgumentCaptor.forClass(HttpRequest.class);
        verify(client).send(sent.capture(), any(HttpResponse.BodyHandler.class));
        var body = new ByteArrayOutputStream();
        sent.getValue().bodyPublisher().orElseThrow().subscribe(new Flow.Subscriber<ByteBuffer>() {
            public void onSubscribe(Flow.Subscription subscription) { subscription.request(Long.MAX_VALUE); }
            public void onNext(ByteBuffer buffer) { byte[] bytes = new byte[buffer.remaining()]; buffer.get(bytes); body.writeBytes(bytes); }
            public void onError(Throwable failure) { throw new AssertionError(failure); }
            public void onComplete() { }
        });
        var json = new ObjectMapper().readTree(body.toString(java.nio.charset.StandardCharsets.UTF_8));
        assertThat(json.path("system").asString()).contains("Trusted stage policy");
        assertThat(json.path("messages").size()).isEqualTo(1);
        assertThat(json.path("messages").path(0).path("role").asString()).isEqualTo("user");
        assertThat(json.path("messages").path(0).path("content").path(0).path("source").path("media_type").asString()).isEqualTo("image/png");
        assertThat(json.path("messages").path(0).path("content").path(1).path("text").asString()).isEqualTo("Describe the picture");
    }
    @Test void normalizesHttpAndTimeoutFailuresWithoutParsingPrivateBodies() {
        assertThat(AiProviderFailureKind.httpStatus(429)).isEqualTo(AiProviderFailureKind.RATE_LIMIT);
        assertThat(AiProviderFailureKind.httpStatus(401)).isEqualTo(AiProviderFailureKind.AUTHENTICATION);
        assertThat(AiProviderFailureKind.httpStatus(400)).isEqualTo(AiProviderFailureKind.INVALID_REQUEST);
        assertThat(new AiProviderException("safe", new java.net.http.HttpTimeoutException("private")).kind()).isEqualTo(AiProviderFailureKind.TIMEOUT);
    }
}
