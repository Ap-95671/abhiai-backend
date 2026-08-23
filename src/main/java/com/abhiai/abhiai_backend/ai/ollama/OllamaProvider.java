package com.abhiai.abhiai_backend.ai.ollama;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.abhiai.abhiai_backend.ai.AiChatMessage;
import com.abhiai.abhiai_backend.ai.AiChatRequest;
import com.abhiai.abhiai_backend.ai.AiCompletion;
import com.abhiai.abhiai_backend.ai.AiProvider;
import com.abhiai.abhiai_backend.exception.AiProviderException;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

@Service
@ConditionalOnProperty(prefix = "app.ai", name = "provider", havingValue = "ollama")
public class OllamaProvider implements AiProvider {
    @Override public String providerName(){return "ollama";}@Override public String modelName(){return properties.getModel();}

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final OllamaProperties properties;

    public OllamaProvider(
            @Qualifier("aiHttpClient") HttpClient httpClient,
            ObjectMapper objectMapper,
            OllamaProperties properties) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public AiCompletion generate(AiChatRequest request) {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(chatUrl()))
                .timeout(properties.getRequestTimeout())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(buildRequestBody(request)))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new AiProviderException("Ollama could not complete the request. Start Ollama and pull the configured model.");
            }
            return new AiCompletion(extractAssistantText(objectMapper.readTree(response.body())));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AiProviderException("Ollama request was interrupted", exception);
        } catch (IOException exception) {
            throw new AiProviderException("Ollama is unavailable. Start Ollama on this computer and pull the configured model.", exception);
        }
    }

    static String extractAssistantText(JsonNode response) {
        String content = response.path("message").path("content").asString();
        if (content.isBlank()) {
            throw new AiProviderException("Ollama returned no assistant message");
        }
        return content;
    }

    private String buildRequestBody(AiChatRequest request) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("model", properties.getModel());
        payload.put("stream", false);
        ArrayNode messages = payload.putArray("messages");
        messages.addObject().put("role", "system").put("content", properties.getInstructions());
        for (AiChatMessage message : request.messages()) {
            messages.addObject()
                    .put("role", message.role().name().toLowerCase(Locale.ROOT))
                    .put("content", message.content());
        }
        return objectMapper.writeValueAsString(payload);
    }

    private String chatUrl() {
        String baseUrl = properties.getBaseUrl();
        return (baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl) + "/api/chat";
    }
}
