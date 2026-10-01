package com.abhiai.abhiai_backend.assistant;

import java.net.URI;
import java.net.http.*;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.*;
import com.abhiai.abhiai_backend.ai.gemini.GeminiProperties;

/** Avatar-only transport. No prompts, history or user questions are sent to LiveAvatar. */
@Service
public class LiveAvatarService {
    private static final Logger log = LoggerFactory.getLogger(LiveAvatarService.class);
    public record Session(String id, String livekitUrl, String livekitToken, String wsUrl) {}
    public record Audio(String data, String mimeType) {}
    private record Lease(UUID owner, String providerId, Instant expires) {}
    private final Map<UUID,Lease> leases = new HashMap<>();
    private final HttpClient http;
    private final ObjectMapper mapper;
    private final GeminiProperties gemini;
    private final AssistantProperties settings;
    @Value("${app.assistant.liveavatar.api-key:}") private String key;
    @Value("${app.assistant.liveavatar.avatar-id:}") private String avatar;
    @Value("${app.assistant.liveavatar.max-session-duration-seconds:120}") private int maxSessionDuration;
    @Value("${app.assistant.tts-model:gemini-3.8-flash-tts}") private String ttsModel;
    public LiveAvatarService(@Qualifier("aiHttpClient") HttpClient http, ObjectMapper mapper,
                             GeminiProperties gemini, AssistantProperties settings) {
        this.http=http; this.mapper=mapper; this.gemini=gemini; this.settings=settings;
    }
    public synchronized Session create(UUID owner, UUID id) {
        if(key.isBlank() || avatar.isBlank()) {
            log.warn("LiveAvatar configuration missing: apiKeyPresent={}, avatarIdPresent={}", !key.isBlank(), !avatar.isBlank());
            throw unavailable();
        }
        if(maxSessionDuration < 1) {
            log.warn("LiveAvatar configuration invalid: session duration must be positive");
            throw unavailable();
        }
        if(leases.containsKey(id)) throw new AssistantException(HttpStatus.CONFLICT,"Avatar session already exists.");
        for(var entry:new ArrayList<>(leases.entrySet())) if(entry.getValue().owner().equals(owner)) close(owner,entry.getKey());
        var data=call("https://api.liveavatar.com/v1/sessions/token","X-API-KEY",key,
            Map.of("mode","LITE","avatar_id",avatar,"max_session_duration",maxSessionDuration)).path("data");
        String providerId=data.path("session_id").asString("");
        String token=data.path("session_token").asString("");
        if(providerId.isBlank() || token.isBlank()) { log.warn("LiveAvatar token response missing required fields"); throw unavailable(); }
        leases.put(id,new Lease(owner,providerId,Instant.now().plusSeconds(maxSessionDuration + 30L)));
        try {
            var started=call("https://api.liveavatar.com/v1/sessions/start","Authorization","Bearer "+token,Map.of()).path("data");
            String url=started.path("livekit_url").asString(""), roomToken=started.path("livekit_client_token").asString(""), ws=started.path("ws_url").asString("");
            if(url.isBlank() || roomToken.isBlank() || ws.isBlank()) { log.warn("LiveAvatar start response missing WebRTC/control credentials"); throw unavailable(); }
            return new Session(id.toString(),url,roomToken,ws);
        } catch(RuntimeException failure) {
            try { close(owner,id); } catch(RuntimeException ignored) { }
            throw unavailable();
        }
    }
    public synchronized void close(UUID owner,UUID id) {
        var lease=leases.get(id);
        if(lease==null || !lease.owner().equals(owner)) return;
        // Retain failed stops for the reaper; the provider duration is a final backstop.
        call("https://api.liveavatar.com/v1/sessions/stop","X-API-KEY",key,
            Map.of("session_id",lease.providerId(),"reason","USER_CLOSED"));
        leases.remove(id);
    }
    @Scheduled(fixedDelay=30000)
    synchronized void expire() {
        for(var entry:new ArrayList<>(leases.entrySet())) if(entry.getValue().expires().isBefore(Instant.now())) {
            try { close(entry.getValue().owner(),entry.getKey()); }
            catch(RuntimeException ignored) { if(entry.getValue().expires().plusSeconds(300).isBefore(Instant.now())) leases.remove(entry.getKey()); }
        }
    }
    public Audio speech(String text) {
        if(gemini.getApiKey()==null || gemini.getApiKey().isBlank()) throw unavailable();
        var result=call(gemini.getBaseUrl().replaceAll("/$","")+"/interactions","x-goog-api-key",gemini.getApiKey(),
            Map.of("model",ttsModel,"input",List.of(Map.of("type","user_input","content",List.of(Map.of("type","text","text",text)))),
                "response_format",Map.of("type","audio"),"generation_config",Map.of("speech_config",List.of(Map.of("voice",settings.getVoice())))));
        Audio audio=null;
        for(var step:result.path("steps")) for(var part:step.path("content")) if(part.path("type").asString().equals("audio"))
            audio=new Audio(part.path("data").asString(),part.path("mime_type").asString("audio/wav"));
        if(audio==null || audio.data().isBlank() || audio.data().length()>16000000) throw unavailable();
        return audio;
    }
    private JsonNode call(String url,String header,String secret,Object body) {
        try {
            var builder=HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(45));
            if (url.startsWith("https://api.liveavatar.com/")) builder.header("User-Agent", "AbhiAI-LiveAvatar/1.0");
            var request=builder
                .header(header,secret).header("Content-Type","application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build();
            var response=http.send(request,HttpResponse.BodyHandlers.ofString());
            if(response.statusCode()/100!=2) {
                // Classify only. Provider bodies can echo secrets and must never be logged.
                String detail=response.body().toLowerCase(Locale.ROOT);
                String reason=detail.contains("max_session_duration") ? "session duration exceeds provider/account limit"
                    : response.statusCode()==401 ? "authentication rejected"
                    : response.statusCode()==403 ? "access rejected by provider or gateway"
                    : detail.contains("avatar") ? "avatar configuration rejected"
                    : detail.contains("credit") || response.statusCode()==429 ? "credits/quota/rate limit"
                    : "provider request failed";
                log.warn("Avatar transport {} failed: HTTP {}, {}", URI.create(url).getPath(), response.statusCode(), reason);
                throw unavailable();
            }
            return mapper.readTree(response.body());
        } catch(InterruptedException e) { Thread.currentThread().interrupt(); log.warn("Avatar transport interrupted at {}", URI.create(url).getPath()); throw unavailable(); }
          catch(java.io.IOException e) { log.warn("Avatar transport network/response failure at {}", URI.create(url).getPath()); throw unavailable(); }
    }
    private static AssistantException unavailable() { return new AssistantException(HttpStatus.SERVICE_UNAVAILABLE,
        "Astra couldn't start the live avatar. Try again, or continue with standard voice."); }
}
