package com.abhiai.abhiai_backend.assistant;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpStatus;
import com.abhiai.abhiai_backend.ai.gemini.GeminiProperties;
import tools.jackson.databind.ObjectMapper;

/** Provision one-use Gemini Live tokens; the permanent Gemini key never leaves the server. */
@Component
public class RealtimeGateway {
    private final HttpClient http;
    private final ObjectMapper mapper;
    private final GeminiProperties gemini;
    private final AssistantProperties settings;
    public RealtimeGateway(@org.springframework.beans.factory.annotation.Qualifier("aiHttpClient") HttpClient http,
                           ObjectMapper mapper, GeminiProperties gemini, AssistantProperties settings) {
        this.http = http; this.mapper = mapper; this.gemini = gemini; this.settings = settings;
    }
    public boolean available() {
        return settings.isVoiceEnabled() && gemini.getApiKey() != null && !gemini.getApiKey().isBlank();
    }
    public String create(Instant expiresAt) {
        return create(expiresAt,"STANDARD");
    }
    public String create(Instant expiresAt,String mode) {
        if (!available()) throw unavailable();
        Map<String, Object> setup = Map.of(
                "model", "models/" + settings.getModel(),
                "tools", List.of(Map.of("functionDeclarations", AssistantToolRegistry.declarations())),
                "generationConfig", Map.of("responseModalities", List.of("AUDIO"), "maxOutputTokens", 2048,
                        "speechConfig", Map.of("voiceConfig", Map.of("prebuiltVoiceConfig", Map.of("voiceName", settings.getVoice())))),
                "systemInstruction", Map.of("parts", List.of(Map.of("text", AssistantPersonality.INSTRUCTIONS+"\n"+AssistantPreferencesService.style(mode)))),
                "inputAudioTranscription", Map.of(), "outputAudioTranscription", Map.of(),
                "realtimeInputConfig", Map.of("automaticActivityDetection", Map.of("silenceDurationMs", 700)));
        // Raw REST uses bidiGenerateContentSetup. liveConnectConstraints is an SDK input field.
        // Omit fieldMask to lock the entire setup at Google, including model and system instructions.
        Map<String, Object> body = Map.of(
                "uses", 1, "expireTime", expiresAt.toString(),
                "newSessionExpireTime", Instant.now().plusSeconds(60).toString(),
                "bidiGenerateContentSetup", setup);
        try {
            var request = HttpRequest.newBuilder(URI.create("https://generativelanguage.googleapis.com/v1beta/auth_tokens"))
                    .timeout(Duration.ofSeconds(20))
                    .header("x-goog-api-key", gemini.getApiKey()).header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build();
            var response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 429) throw new AssistantException(HttpStatus.TOO_MANY_REQUESTS,
                    "Gemini Live quota reached. Wait and try again, or continue by text.");
            if (response.statusCode() / 100 != 2) throw unavailable();
            String token = mapper.readTree(response.body()).path("name").asString("");
            if (!token.startsWith("auth_tokens/") || token.length() > 16384) throw unavailable();
            return token;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt(); throw unavailable();
        } catch (java.io.IOException exception) { throw unavailable(); }
    }
    private static AssistantException unavailable() {
        return new AssistantException(HttpStatus.SERVICE_UNAVAILABLE,
                "Gemini Live is unavailable. Check the Gemini key and Live API access, or continue by text.");
    }
}
