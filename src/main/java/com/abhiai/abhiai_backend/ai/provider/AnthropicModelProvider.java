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
                throw new AiProviderException("Anthropic could not complete the request.",
                        com.abhiai.abhiai_backend.exception.AiProviderFailureKind.httpStatus(response.statusCode()));
            JsonNode root = mapper.readTree(response.body());
            StringBuilder text = new StringBuilder();
            root.path("content").forEach(node -> { if ("text".equals(node.path("type").asString())) text.append(node.path("text").asString()); });
            if (text.isEmpty()) throw new AiProviderException("Anthropic returned no assistant message");
            return new AiCompletion(text.toString(), "anthropic", root.path("model").asString(request.providerModelId()),
                    root.path("stop_reason").asString(null), root.path("usage").path("input_tokens").isNumber() ? root.path("usage").path("input_tokens").asInt() : null,
                    root.path("usage").path("output_tokens").isNumber() ? root.path("usage").path("output_tokens").asInt() : null, 0, false);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt(); throw new AiProviderException("Anthropic request was interrupted", exception);
        } catch (IOException exception) { throw new AiProviderException("Anthropic request failed", exception); }
    }

    private String body(AiChatRequest request) {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("model", request.providerModelId() == null ? properties.getModel() : request.providerModelId());
        payload.put("max_tokens", 4096);
        StringBuilder system = new StringBuilder(properties.getInstructions() == null ? "" : properties.getInstructions());
        request.messages().stream().filter(m -> m.role() == com.abhiai.abhiai_backend.entity.MessageRole.SYSTEM)
                .forEach(m -> system.append("\n").append(m.content()));
        payload.put("system", system.toString());
        ArrayNode messages = payload.putArray("messages");
        for (int index = 0; index < request.messages().size(); index++) {
            var message = request.messages().get(index);
            if (message.role() == com.abhiai.abhiai_backend.entity.MessageRole.SYSTEM) continue;
            var item = messages.addObject().put("role", message.role().name().toLowerCase(Locale.ROOT));
            if (index == request.messages().size() - 1 && message.role() == com.abhiai.abhiai_backend.entity.MessageRole.USER
                    && !request.attachments().isEmpty()) {
                var content = item.putArray("content");
                for (var attachment : request.attachments()) {
                    var source = content.addObject().put("type", "image").putObject("source");
                    source.put("type", "base64").put("media_type", attachment.contentType())
                            .put("data", java.util.Base64.getEncoder().encodeToString(attachment.content()));
                }
                content.addObject().put("type", "text").put("text", message.content());
            } else item.put("content", message.content());
        }
        return mapper.writeValueAsString(payload);
    }
    private String url() {
        String base = properties.getBaseUrl();
        return (base.endsWith("/") ? base.substring(0, base.length() - 1) : base) + "/messages";
    }
}
