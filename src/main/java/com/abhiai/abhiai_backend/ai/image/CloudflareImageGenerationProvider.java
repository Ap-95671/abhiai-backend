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
public class CloudflareImageGenerationProvider implements ImageGenerationProvider {

    private static final int MAX_GENERATED_IMAGE_BYTES = 15 * 1024 * 1024;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final CloudflareImageGenerationProperties properties;

    public CloudflareImageGenerationProvider(
            @Qualifier("aiHttpClient") HttpClient httpClient,
            ObjectMapper objectMapper,
            CloudflareImageGenerationProperties properties) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public String providerName() { return "cloudflare"; }

    @Override
    public String modelName() { return properties.getModel(); }

    @Override
    public boolean configured() {
        return hasText(properties.getAccountId()) && hasText(properties.getApiToken());
    }

    @Override
    public GeneratedImage generate(String prompt) {
        if (!configured()) {
            throw new AiProviderUnavailableException("Cloudflare image generation is not configured.");
        }
        validateConfiguration();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(runUrl()))
                .timeout(properties.getRequestTimeout())
                .header("Authorization", "Bearer " + properties.getApiToken())
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
            throw new AiProviderException("Cloudflare image generation is temporarily unavailable", exception);
        }
    }

    GeneratedImage extractGeneratedImage(JsonNode response) {
        JsonNode result = response.path("result");
        String encoded = result.path("image").asString();
        if (encoded.isBlank()) encoded = response.path("image").asString();
        if (encoded.isBlank()) {
            throw new AiProviderException("Cloudflare returned no generated image");
        }
        try {
            byte[] content = Base64.getDecoder().decode(encoded);
            if (content.length == 0 || content.length > MAX_GENERATED_IMAGE_BYTES) {
                throw new AiProviderException("The generated image had an invalid size");
            }
            return new GeneratedImage(content, "image/jpeg", properties.getModel());
        } catch (IllegalArgumentException exception) {
            throw new AiProviderException("Cloudflare returned invalid image data", exception);
        }
    }

    String buildRequestBody(String prompt) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("prompt", prompt);
        payload.put("steps", Math.max(1, Math.min(8, properties.getSteps())));
        return objectMapper.writeValueAsString(payload);
    }

    String providerFailureMessage(int statusCode, String body) {
        JsonNode response;
        try {
            response = objectMapper.readTree(body);
        } catch (RuntimeException exception) {
            response = objectMapper.createObjectNode();
        }
        JsonNode firstError = response.path("errors").path(0);
        int errorCode = firstError.path("code").asInt(0);
        String providerMessage = firstError.path("message").asString();
        String normalized = providerMessage.toLowerCase();
        if (statusCode == 401 || statusCode == 403) {
            return "Cloudflare rejected the Workers AI credentials or permissions";
        }
        if (statusCode == 429 && (errorCode == 3036 || normalized.contains("daily free allocation"))) {
            return "Image generation is temporarily unavailable because the free Cloudflare AI quota has been exhausted. Please try again after the daily quota resets.";
        }
        if (statusCode == 429) {
            return "Cloudflare image generation is temporarily rate limited. Please try again shortly.";
        }
        if (statusCode == 400 || statusCode == 404) {
            return "The configured Cloudflare image model is unavailable or invalid";
        }
        if (statusCode >= 500) {
            return "Cloudflare image generation is temporarily unavailable";
        }
        return "Cloudflare could not generate the image (HTTP " + statusCode + ")";
    }

    private void validateConfiguration() {
        if (!properties.getAccountId().matches("[A-Za-z0-9_-]{3,128}")) {
            throw new AiProviderUnavailableException("The Cloudflare account ID is invalid.");
        }
        if (!properties.getModel().startsWith("@cf/")
                || properties.getModel().contains("?")
                || properties.getModel().contains("#")
                || properties.getModel().chars().anyMatch(Character::isWhitespace)) {
            throw new AiProviderUnavailableException("The Cloudflare Workers AI model is invalid.");
        }
    }

    private String runUrl() {
        String base = properties.getBaseUrl().endsWith("/")
                ? properties.getBaseUrl().substring(0, properties.getBaseUrl().length() - 1)
                : properties.getBaseUrl();
        return base + "/accounts/" + properties.getAccountId() + "/ai/run/" + properties.getModel();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
