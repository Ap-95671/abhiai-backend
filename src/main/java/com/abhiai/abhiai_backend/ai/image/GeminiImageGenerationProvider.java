package com.abhiai.abhiai_backend.ai.image;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.abhiai.abhiai_backend.exception.AiProviderException;
import com.abhiai.abhiai_backend.exception.AiProviderUnavailableException;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

@Service
public class GeminiImageGenerationProvider implements ImageGenerationProvider {

    private static final int MAX_GENERATED_IMAGE_BYTES = 15 * 1024 * 1024;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final GeminiImageGenerationProperties properties;

    public GeminiImageGenerationProvider(
            @Qualifier("aiHttpClient") HttpClient httpClient,
            ObjectMapper objectMapper,
            GeminiImageGenerationProperties properties) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public String providerName() { return "gemini"; }

    @Override
    public String modelName() { return properties.getModel(); }

    @Override
    public boolean configured() {
        return properties.getApiKey() != null && !properties.getApiKey().isBlank();
    }

    @Override
    public GeneratedImage generate(String prompt) {
        if (!configured()) {
            throw new AiProviderUnavailableException();
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(interactionsUrl()))
                .timeout(properties.getRequestTimeout())
                .header("x-goog-api-key", properties.getApiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(buildRequestBody(prompt)))
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new AiProviderException(providerFailureMessage(response.statusCode(), response.body()));
            }
            return extractGeneratedImage(objectMapper.readTree(response.body()));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AiProviderException("Image generation was interrupted", exception);
        } catch (IOException exception) {
            throw new AiProviderException("The image-generation provider could not be reached", exception);
        }
    }

    GeneratedImage extractGeneratedImage(JsonNode response) {
        for (JsonNode step : response.path("steps")) {
            if (!"model_output".equals(step.path("type").asString())) continue;
            for (JsonNode block : step.path("content")) {
                if (!"image".equals(block.path("type").asString())) continue;
                String encoded = block.path("data").asString();
                String contentType = block.path("mime_type").asString("image/jpeg");
                if (encoded.isBlank()) continue;
                try {
                    byte[] content = Base64.getDecoder().decode(encoded);
                    if (content.length == 0 || content.length > MAX_GENERATED_IMAGE_BYTES) {
                        throw new AiProviderException("The generated image had an invalid size");
                    }
                    if (!contentType.equals("image/png") && !contentType.equals("image/jpeg") && !contentType.equals("image/webp")) {
                        throw new AiProviderException("The image provider returned an unsupported image format");
                    }
                    return new GeneratedImage(content, contentType, properties.getModel());
                } catch (IllegalArgumentException exception) {
                    throw new AiProviderException("The image provider returned invalid image data", exception);
                }
            }
        }
        throw new AiProviderException("The image provider returned no generated image");
    }

    private String buildRequestBody(String prompt) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("model", properties.getModel());
        payload.putArray("input").addObject().put("type", "text").put("text", prompt);
        payload.putObject("response_format")
                .put("type", "image")
                .put("mime_type", "image/jpeg")
                .put("aspect_ratio", "1:1")
                .put("image_size", "1K");
        return objectMapper.writeValueAsString(payload);
    }

    private String interactionsUrl() {
        String base = properties.getBaseUrl().endsWith("/")
                ? properties.getBaseUrl().substring(0, properties.getBaseUrl().length() - 1)
                : properties.getBaseUrl();
        return base + "/interactions";
    }

    private String providerFailureMessage(int statusCode, String body) {
        String detail = objectMapper.readTree(body).path("error").path("message").asString();
        if (statusCode == 401 || statusCode == 403) {
            return "Gemini rejected the image-generation API key or project permissions";
        }
        if (statusCode == 429) {
            return "Gemini image-generation quota is currently exhausted. Please retry later or check billing and rate limits.";
        }
        return detail.isBlank()
                ? "Gemini could not generate the image (HTTP " + statusCode + ")"
                : "Gemini could not generate the image: " + detail;
    }
}
