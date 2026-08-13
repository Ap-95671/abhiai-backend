package com.abhiai.abhiai_backend.ai.openai;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.abhiai.abhiai_backend.ai.AiChatMessage;
import com.abhiai.abhiai_backend.ai.AiChatRequest;
import com.abhiai.abhiai_backend.ai.AiCompletion;
import com.abhiai.abhiai_backend.ai.AiProvider;
import com.abhiai.abhiai_backend.exception.AiProviderException;
import com.abhiai.abhiai_backend.exception.AiProviderUnavailableException;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

@Service
public class OpenAiResponsesProvider implements AiProvider {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final OpenAiProperties properties;

    public OpenAiResponsesProvider(
            @Qualifier("openAiHttpClient") HttpClient httpClient,
            ObjectMapper objectMapper,
            OpenAiProperties properties) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public AiCompletion generate(AiChatRequest request) {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new AiProviderUnavailableException();
        }

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(responsesUrl()))
                .timeout(properties.getRequestTimeout())
                .header("Authorization", "Bearer " + properties.getApiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(buildRequestBody(request)))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new AiProviderException("The AI provider could not complete the request");
            }

            return new AiCompletion(extractAssistantText(objectMapper.readTree(response.body())));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AiProviderException("The AI provider request was interrupted", exception);
        } catch (IOException exception) {
            throw new AiProviderException("The AI provider request failed", exception);
        }
    }

    static String extractAssistantText(JsonNode response) {
        StringBuilder content = new StringBuilder();

        for (JsonNode outputItem : response.path("output")) {
            if (!"message".equals(outputItem.path("type").asString())) {
                continue;
            }

            for (JsonNode contentItem : outputItem.path("content")) {
                if ("output_text".equals(contentItem.path("type").asString())) {
                    content.append(contentItem.path("text").asString());
                }
            }
        }

        if (content.isEmpty()) {
            throw new AiProviderException("The AI provider returned no assistant message");
        }

        return content.toString();
    }

    private String buildRequestBody(AiChatRequest request) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("model", properties.getModel());
        payload.put("instructions", properties.getInstructions());
        payload.put("store", false);
        payload.putObject("reasoning").put("effort", properties.getReasoningEffort());

        ArrayNode input = payload.putArray("input");
        for (AiChatMessage message : request.messages()) {
            input.addObject()
                    .put("role", message.role().name().toLowerCase(Locale.ROOT))
                    .put("content", message.content());
        }

        return objectMapper.writeValueAsString(payload);
    }

    private String responsesUrl() {
        String baseUrl = properties.getBaseUrl();
        return (baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl) + "/responses";
    }
}
