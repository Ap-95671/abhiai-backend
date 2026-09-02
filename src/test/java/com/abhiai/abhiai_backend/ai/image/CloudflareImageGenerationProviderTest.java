package com.abhiai.abhiai_backend.ai.image;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.net.http.HttpClient;
import java.util.Base64;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;

class CloudflareImageGenerationProviderTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void extractsImageFromWorkersAiEnvelope() {
        CloudflareImageGenerationProperties properties = new CloudflareImageGenerationProperties();
        CloudflareImageGenerationProvider provider = provider(properties);
        byte[] expected = new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff};
        String response = "{\"success\":true,\"result\":{\"image\":\"%s\"}}"
                .formatted(Base64.getEncoder().encodeToString(expected));

        GeneratedImage result = provider.extractGeneratedImage(objectMapper.readTree(response));

        assertArrayEquals(expected, result.content());
        assertEquals("image/jpeg", result.contentType());
        assertEquals("@cf/black-forest-labs/flux-1-schnell", result.model());
    }

    @Test
    void buildsTheDocumentedFluxRequestAndClampsSteps() {
        CloudflareImageGenerationProperties properties = new CloudflareImageGenerationProperties();
        properties.setSteps(99);
        CloudflareImageGenerationProvider provider = provider(properties);

        var request = objectMapper.readTree(provider.buildRequestBody("A quiet future city"));

        assertEquals("A quiet future city", request.path("prompt").asString());
        assertEquals(8, request.path("steps").asInt());
        assertFalse(request.has("model"));
    }

    @Test
    void returnsAFreeTierSpecificQuotaMessage() {
        CloudflareImageGenerationProvider provider = provider(new CloudflareImageGenerationProperties());
        String body = "{\"errors\":[{\"code\":3036,\"message\":\"daily free allocation exhausted\"}]}";

        assertEquals(
                "Image generation is temporarily unavailable because the free Cloudflare AI quota has been exhausted. Please try again after the daily quota resets.",
                provider.providerFailureMessage(429, body));
    }

    private CloudflareImageGenerationProvider provider(CloudflareImageGenerationProperties properties) {
        return new CloudflareImageGenerationProvider(HttpClient.newHttpClient(), objectMapper, properties);
    }
}
