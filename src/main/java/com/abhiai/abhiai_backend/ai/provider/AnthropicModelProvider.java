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

public class AnthropicModelProvider implements ModelProvider {
    private final HttpClient client;
    private final ObjectMapper mapper;
    private final MultiProviderProperties.Provider properties;

    public AnthropicModelProvider(HttpClient client, ObjectMapper mapper, MultiProviderProperties.Provider properties) {
        this.client = client; this.mapper = mapper; this.properties = properties;
    }
    @Override public String providerName() { return "anthropic"; }
    @Override public String modelName() { return properties.getModel(); }
    @Override public boolean configured() { return properties.getApiKey() != null && !properties.getApiKey().isBlank(); }
    @Override public boolean supportsImageUnderstanding() { return true; }

    @Override
    public AiCompletion generate(AiChatRequest request) {
        if (!configured()) throw new AiProviderUnavailableException();
        HttpRequest httpRequest = HttpRequest.newBuilder().uri(URI.create(url()))
                .timeout(properties.getRequestTimeout())
                .header("x-api-key", properties.getApiKey())
                .header("anthropic-version", "2023-06-01")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body(request))).build();
        try {
            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300)
                throw new AiProviderException(response.statusCode() == 429 ? "Anthropic rate limit or quota was reached." : "Anthropic could not complete the request.");
            JsonNode root = mapper.readTree(response.body());
            StringBuilder text = new StringBuilder();
            root.path("content").forEach(node -> { if ("text".equals(node.path("type").asString())) text.append(node.path("text").asString()); });
            if (text.isEmpty()) throw new AiProviderException("Anthropic returned no assistant message");
            return new AiCompletion(text.toString(), "anthropic", root.path("model").asString(request.providerModelId()),
                    root.path("stop_reason").asString(null), root.path("usage").path("input_tokens").asInt(),
                    root.path("usage").path("output_tokens").asInt(), 0, false);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt(); throw new AiProviderException("Anthropic request was interrupted", exception);
        } catch (IOException exception) { throw new AiProviderException("Anthropic request failed", exception); }
    }

    private String body(AiChatRequest request) {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("model", request.providerModelId() == null ? properties.getModel() : request.providerModelId());
        payload.put("max_tokens", 4096);
        if (properties.getInstructions() != null) payload.put("system", properties.getInstructions());
        ArrayNode messages = payload.putArray("messages");
        request.messages().forEach(message -> messages.addObject()
                .put("role", message.role().name().toLowerCase(Locale.ROOT)).put("content", message.content()));
        return mapper.writeValueAsString(payload);
    }
    private String url() {
        String base = properties.getBaseUrl();
        return (base.endsWith("/") ? base.substring(0, base.length() - 1) : base) + "/messages";
    }
}
