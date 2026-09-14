package com.abhiai.abhiai_backend.assistant;

import java.net.http.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Flow;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import com.abhiai.abhiai_backend.ai.gemini.GeminiProperties;
import java.time.Instant;
import tools.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

class RealtimeGatewayTest {
    @Test void usesGeminiCredentialAndConstrainsOneUseToken() throws Exception {
        var http = mock(HttpClient.class);
        HttpResponse<String> response = mockResponse(200, "{\"name\":\"auth_tokens/short-lived\"}");
        when(http.send(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(response);
        var properties = new AssistantProperties(); properties.setVoiceEnabled(true);
        var openAi = new GeminiProperties(); openAi.setApiKey("server-only-test-key");
        var gateway = new RealtimeGateway(http, new ObjectMapper(), openAi, properties);
        var result = gateway.create(Instant.now().plusSeconds(900));
        assertEquals("auth_tokens/short-lived", result); assertFalse(result.toString().contains("server-only-test-key"));
        var capture = ArgumentCaptor.forClass(HttpRequest.class);
        verify(http).send(capture.capture(), any());
        var request = capture.getValue();
        assertEquals("https://generativelanguage.googleapis.com/v1beta/auth_tokens", request.uri().toString());
        assertEquals("server-only-test-key", request.headers().firstValue("x-goog-api-key").orElseThrow());
        var body = new StringBuilder();
        request.bodyPublisher().orElseThrow().subscribe(new Flow.Subscriber<ByteBuffer>() {
            public void onSubscribe(Flow.Subscription subscription) { subscription.request(Long.MAX_VALUE); }
            public void onNext(ByteBuffer buffer) { body.append(StandardCharsets.UTF_8.decode(buffer)); }
            public void onError(Throwable error) { fail(error); }
            public void onComplete() {}
        });
        assertTrue(body.toString().contains("bidiGenerateContentSetup")); assertTrue(body.toString().contains(AssistantPersonality.INSTRUCTIONS.trim().split("\n")[0]));
        assertFalse(body.toString().contains("server-only-test-key"));
        assertTrue(body.toString().contains("\"uses\":1"));
        assertTrue(body.toString().contains("newSessionExpireTime"));
        assertTrue(body.toString().contains("expireTime"));
        assertTrue(body.toString().contains("gemini-3.1-flash-live-preview"));
    }
    @Test void redactsProviderErrorsAndDisabledVoiceNeverCallsProvider() throws Exception {
        var http = mock(HttpClient.class); var properties = new AssistantProperties(); properties.setVoiceEnabled(false); var openAi = new GeminiProperties();
        openAi.setApiKey("secret");
        var gateway = new RealtimeGateway(http, new ObjectMapper(), openAi, properties);
        assertThrows(AssistantException.class, () -> gateway.create(Instant.now().plusSeconds(900))); verifyNoInteractions(http);
        properties.setVoiceEnabled(true);
        var rejected = mockResponse(401, "private provider secret stack trace");
        when(http.send(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
            .thenReturn(rejected);
        var error = assertThrows(AssistantException.class, () -> gateway.create(Instant.now().plusSeconds(900)));
        assertFalse(error.getMessage().contains("secret")); assertTrue(error.getMessage().contains("text"));
    }
    @SuppressWarnings("unchecked")
    private HttpResponse<String> mockResponse(int status, String body) {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status); when(response.body()).thenReturn(body); return response;
    }
}
