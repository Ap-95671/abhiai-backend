package com.abhiai.abhiai_backend.ai.gemini;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.abhiai.abhiai_backend.ai.AiChatMessage;
import com.abhiai.abhiai_backend.ai.AiChatRequest;
import com.abhiai.abhiai_backend.ai.AiCompletion;
import com.abhiai.abhiai_backend.ai.ModelProvider;
import com.abhiai.abhiai_backend.entity.MessageRole;
import com.abhiai.abhiai_backend.exception.AiProviderException;
import com.abhiai.abhiai_backend.exception.AiProviderUnavailableException;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

@Service
public class GeminiProvider implements ModelProvider {
    @Override public boolean supportsImageUnderstanding(){return true;}
    @Override public String providerName(){return "gemini";}@Override public String modelName(){return properties.getModel();}@Override public boolean configured(){return properties.getApiKey()!=null&&!properties.getApiKey().isBlank();}

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final GeminiProperties properties;

    public GeminiProvider(
            @Qualifier("aiHttpClient") HttpClient httpClient,
            ObjectMapper objectMapper,
            GeminiProperties properties) {
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
                .uri(URI.create(generateContentUrl(request)))
                .timeout(properties.getRequestTimeout())
                .header("x-goog-api-key", properties.getApiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(buildRequestBody(request)))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new AiProviderException(providerFailureMessage(response));
            }
            return new AiCompletion(extractAssistantText(objectMapper.readTree(response.body())));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AiProviderException("Gemini request was interrupted", exception);
        } catch (IOException exception) {
            throw new AiProviderException("Gemini request failed", exception);
        }
    }

    @Override
    public AiCompletion generateStream(AiChatRequest request, Consumer<String> onTextChunk) {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new AiProviderUnavailableException();
        }
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(streamGenerateContentUrl(request)))
                .timeout(properties.getRequestTimeout())
                .header("x-goog-api-key", properties.getApiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(buildRequestBody(request)))
                .build();
        StringBuilder completion = new StringBuilder();
        try {
            HttpResponse<java.util.stream.Stream<String>> response = httpClient.send(
                    httpRequest, HttpResponse.BodyHandlers.ofLines());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                try (java.util.stream.Stream<String> lines = response.body()) {
                    throw new AiProviderException(providerFailureMessage(
                            response.statusCode(),
                            lines.collect(java.util.stream.Collectors.joining("\n"))));
                }
            }
            try (java.util.stream.Stream<String> lines = response.body()) {
                lines.filter(line -> line.startsWith("data: ")).forEach(line -> {
                    String chunk = extractAssistantTextOrEmpty(objectMapper.readTree(line.substring(6)));
                    if (chunk.isEmpty()) {
                        return;
                    }
                    completion.append(chunk);
                    onTextChunk.accept(chunk);
                });
            }
            if (completion.isEmpty()) {
                throw new AiProviderException("Gemini returned no assistant message");
            }
            return new AiCompletion(completion.toString());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AiProviderException("Gemini stream was interrupted", exception);
        } catch (IOException exception) {
            throw new AiProviderException("Gemini stream failed", exception);
        }
    }

    static String extractAssistantText(JsonNode response) {
        String content = extractAssistantTextOrEmpty(response);
        if (content.isEmpty()) {
            throw new AiProviderException("Gemini returned no assistant message");
        }
        return content;
    }

    private static String extractAssistantTextOrEmpty(JsonNode response) {
        StringBuilder content = new StringBuilder();
        for (JsonNode part : response.path("candidates").path(0).path("content").path("parts")) {
            content.append(part.path("text").asString());
        }
        return content.toString();
    }

    private String providerFailureMessage(HttpResponse<String> response) {
        return providerFailureMessage(response.statusCode(), response.body());
    }

    private String providerFailureMessage(int statusCode, String body) {
        String message = objectMapper.readTree(body).path("error").path("message").asString();
        if (!message.isBlank()) {
            return "Gemini could not complete the request: " + message;
        }
        return "Gemini could not complete the request (HTTP " + statusCode + ")";
    }

    private String buildRequestBody(AiChatRequest request) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.putObject("systemInstruction").putArray("parts").addObject()
                .put("text", properties.getInstructions());
        ArrayNode contents = payload.putArray("contents");
        for (int index = 0; index < request.messages().size(); index++) {
            AiChatMessage message = request.messages().get(index);
            ArrayNode parts = contents.addObject()
                    .put("role", message.role() == MessageRole.ASSISTANT ? "model" : "user")
                    .putArray("parts");
            parts.addObject().put("text", message.content());
            if (index == request.messages().size() - 1 && message.role() == MessageRole.USER) {
                for (var attachment : request.attachments()) {
                    parts.addObject().putObject("inlineData")
                            .put("mimeType", attachment.contentType())
                            .put("data", java.util.Base64.getEncoder().encodeToString(attachment.content()));
                }
            }
        }
        return objectMapper.writeValueAsString(payload);
    }

    private String generateContentUrl(AiChatRequest request) {
        String baseUrl = properties.getBaseUrl();
        return (baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl)
                + "/models/" + (request.providerModelId() == null ? properties.getModel() : request.providerModelId()) + ":generateContent";
    }

    private String streamGenerateContentUrl(AiChatRequest request) {
        return generateContentUrl(request).replace(":generateContent", ":streamGenerateContent?alt=sse");
    }
}
