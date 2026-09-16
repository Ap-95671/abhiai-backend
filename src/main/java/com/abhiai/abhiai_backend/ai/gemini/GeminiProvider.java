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
    public AiCompletion generate(AiChatRequest request) { return generate(request, null); }

    public String generateStructured(AiChatRequest request, java.util.Map<String,Object> schema) {
        return generate(request, schema).content();
    }

    private AiCompletion generate(AiChatRequest request, java.util.Map<String,Object> schema) {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new AiProviderUnavailableException();
        }

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(generateContentUrl(request)))
                .timeout(schema == null ? properties.getRequestTimeout() : java.time.Duration.ofSeconds(20))
                .header("x-goog-api-key", properties.getApiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(buildRequestBody(request, schema)))
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
                lines.filter(line -> line.startsWith("data:")).forEach(line -> {
                    var frame = objectMapper.readTree(line.substring(5).trim());
                    String reason = frame.path("candidates").path(0).path("finishReason").asString("");
                    if (!reason.isBlank() && !reason.equals("STOP"))
                        org.slf4j.LoggerFactory.getLogger(getClass()).warn("gemini_stream_finish reason={}", reason.replaceAll("[^A-Z_]", ""));
                    String chunk = extractAssistantTextOrEmpty(frame);
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
            if (!part.path("thought").asBoolean(false)) content.append(part.path("text").asString());
        }
        return content.toString();
    }

    private String providerFailureMessage(HttpResponse<String> response) {
        return providerFailureMessage(response.statusCode(), response.body());
    }

    private String providerFailureMessage(int statusCode, String body) {
        // Provider bodies can include request details. Keep them out of logs and user-visible errors.
        return "Gemini could not complete the request (HTTP " + statusCode + ")";
    }

    String buildRequestBody(AiChatRequest request) { return buildRequestBody(request, null); }
    private String buildRequestBody(AiChatRequest request, java.util.Map<String,Object> schema) {
        ObjectNode payload = objectMapper.createObjectNode();
        ArrayNode systemParts = payload.putObject("systemInstruction").putArray("parts");
        systemParts.addObject().put("text", properties.getInstructions());
        if (schema != null) {
            var config = payload.putObject("generationConfig");
            config.put("responseMimeType", "application/json");
            config.set("responseJsonSchema", objectMapper.valueToTree(schema));
            config.put("maxOutputTokens", 2300);
        }
        ArrayNode contents = payload.putArray("contents");
        for (int index = 0; index < request.messages().size(); index++) {
            AiChatMessage message = request.messages().get(index);
            if (message.role() == MessageRole.SYSTEM) {
                systemParts.addObject().put("text", message.content());
                continue;
            }
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
