package com.abhiai.abhiai_backend.assistant;

import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import com.abhiai.abhiai_backend.security.JwtPrincipal;
import com.abhiai.abhiai_backend.dto.chat.ConversationDetailResponse;

@RestController
@RequestMapping("/api/v1/assistant")
public class AssistantController {
    public record Config(boolean enabled, boolean voiceAvailable) {}
    public record OpenRequest(boolean fresh) {}
    public record SessionRequest(@NotNull UUID id, @NotNull UUID conversationId) {}
    private final AssistantProperties settings;
    private final AssistantPolicy policy;
    private final AssistantConversationService conversations;
    private final RealtimeSessionService sessions;
    private final RealtimeGateway gateway;
    public AssistantController(AssistantProperties settings, AssistantPolicy policy, AssistantConversationService conversations,
                               RealtimeSessionService sessions, RealtimeGateway gateway) {
        this.settings = settings; this.policy = policy; this.conversations = conversations;
        this.sessions = sessions; this.gateway = gateway;
    }
    @GetMapping("/config")
    public Config config() { return new Config(settings.isEnabled(), settings.isEnabled() && gateway.available()); }
    @PostMapping("/conversation")
    public ConversationDetailResponse open(@AuthenticationPrincipal JwtPrincipal principal, @Valid @RequestBody OpenRequest request) {
        policy.request(principal.userId());
        return conversations.open(principal.userId(), request.fresh());
    }
    @PutMapping("/conversations/{id}/transcript")
    public ResponseEntity<Void> transcript(@AuthenticationPrincipal JwtPrincipal principal, @PathVariable UUID id,
            @Valid @RequestBody AssistantConversationService.TranscriptBatch request) {
        policy.request(principal.userId());
        conversations.append(principal.userId(), id, request);
        return ResponseEntity.noContent().build();
    }
    @PostMapping("/sessions")
    public ResponseEntity<RealtimeSessionService.SessionResponse> create(@AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody SessionRequest request) {
        policy.requireEnabled();
        conversations.history(principal.userId(), request.conversationId());
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore())
                .body(sessions.create(principal.userId(), request.id()));
    }
    @DeleteMapping("/sessions/{id}")
    public ResponseEntity<Void> close(@AuthenticationPrincipal JwtPrincipal principal, @PathVariable UUID id) {
        sessions.close(principal.userId(), id);
        return ResponseEntity.noContent().build();
    }
}
