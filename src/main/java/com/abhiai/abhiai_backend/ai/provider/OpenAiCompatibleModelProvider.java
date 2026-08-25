package com.abhiai.abhiai_backend.ai.provider;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Locale;

import com.abhiai.abhiai_backend.ai.AiChatRequest;
import com.abhiai.abhiai_backend.ai.AiCompletion;
import com.abhiai.abhiai_backend.ai.ModelProvider;
import com.abhiai.abhiai_backend.config.MultiProviderProperties;
import com.abhiai.abhiai_backend.exception.AiProviderException;
import com.abhiai.abhiai_backend.exception.AiProviderUnavailableException;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

public class OpenAiCompatibleModelProvider implements ModelProvider {
    private final String providerName;
    private final HttpClient client;
    private final ObjectMapper mapper;
    private final MultiProviderProperties.Provider properties;

    public OpenAiCompatibleModelProvider(String providerName, HttpClient client, ObjectMapper mapper,
                                         MultiProviderProperties.Provider properties) {
        this.providerName = providerName;
        this.client = client;
        this.mapper = mapper;
        this.properties = properties;
    }

    @Override public String providerName() { return providerName; }
    @Override public String modelName() { return properties.getModel(); }
    @Override public boolean configured() { return notBlank(properties.getApiKey()) && notBlank(properties.getBaseUrl()); }

    @Override
    public AiCompletion generate(AiChatRequest request) {
        if (!configured()) throw new AiProviderUnavailableException();
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(url()))
                .timeout(properties.getRequestTimeout())
                .header("Authorization", "Bearer " + properties.getApiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body(request)))
                .build();
        try {
            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300)
                throw new AiProviderException(failureMessage(response.statusCode()));
            JsonNode root = mapper.readTree(response.body());
            String content = root.path("choices").path(0).path("message").path("content").asString();
            if (content.isBlank()) throw new AiProviderException(providerName + " returned no assistant message");
            JsonNode usage = root.path("usage");
            return new AiCompletion(content, providerName,
                    root.path("model").asString(request.providerModelId()),
                    root.path("choices").path(0).path("finish_reason").asString(null),
                    usage.path("prompt_tokens").isNumber() ? usage.path("prompt_tokens").asInt() : null,
                    usage.path("completion_tokens").isNumber() ? usage.path("completion_tokens").asInt() : null,
                    0, false);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AiProviderException(providerName + " request was interrupted", exception);
        } catch (IOException exception) {
            throw new AiProviderException(providerName + " request failed", exception);
        }
    }

    private String body(AiChatRequest request) {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("model", request.providerModelId() == null ? properties.getModel() : request.providerModelId());
        ArrayNode messages = payload.putArray("messages");
        if (notBlank(properties.getInstructions())) messages.addObject().put("role", "system").put("content", properties.getInstructions());
        request.messages().forEach(message -> messages.addObject()
                .put("role", message.role().name().toLowerCase(Locale.ROOT)).put("content", message.content()));
        return mapper.writeValueAsString(payload);
    }

    private String url() {
        String base = properties.getBaseUrl();
        return (base.endsWith("/") ? base.substring(0, base.length() - 1) : base) + "/chat/completions";
    }

    private String failureMessage(int status) {
        if (status == 401 || status == 403) return providerName + " rejected its API credentials.";
        if (status == 429) return providerName + " rate limit or quota was reached.";
        return providerName + " could not complete the request (HTTP " + status + ").";
    }

    private boolean notBlank(String value) { return value != null && !value.isBlank(); }
}
