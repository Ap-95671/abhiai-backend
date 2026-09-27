package com.abhiai.abhiai_backend.assistant;

import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.*;
import com.abhiai.abhiai_backend.security.JwtPrincipal;
import com.abhiai.abhiai_backend.entity.MessageRole;

@RestController
@RequestMapping("/api/v1/assistant/liveavatar")
public class LiveAvatarController {
    public record Create(@NotNull UUID id,@NotNull UUID conversationId) {}
    public record Speech(@NotNull UUID conversationId,@NotNull UUID messageId) {}
    private final LiveAvatarService avatars;
    private final AssistantPolicy policy;
    private final AssistantConversationService conversations;
    public LiveAvatarController(LiveAvatarService avatars,AssistantPolicy policy,AssistantConversationService conversations) {
        this.avatars=avatars;this.policy=policy;this.conversations=conversations;
    }
    @PostMapping("/sessions")
    public ResponseEntity<LiveAvatarService.Session> create(@AuthenticationPrincipal JwtPrincipal user,@Valid @RequestBody Create body) {
        policy.session(user.userId());
        conversations.history(user.userId(),body.conversationId());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(avatars.create(user.userId(),body.id()));
    }
    @DeleteMapping("/sessions/{id}")
    public ResponseEntity<Void> close(@AuthenticationPrincipal JwtPrincipal user,@PathVariable UUID id) {
        avatars.close(user.userId(),id);return ResponseEntity.noContent().build();
    }
    @PostMapping("/speech")
    public ResponseEntity<LiveAvatarService.Audio> speech(@AuthenticationPrincipal JwtPrincipal user,@Valid @RequestBody Speech body) {
        policy.request(user.userId());
        var message=conversations.history(user.userId(),body.conversationId()).messages().stream()
            .filter(m->m.id().equals(body.messageId()) && m.role()==MessageRole.ASSISTANT).findFirst()
            .orElseThrow(()->new AssistantException(HttpStatus.NOT_FOUND,"Assistant reply unavailable."));
        // Read the saved assistant response, never accept a new LLM question or arbitrary client transcript.
        if(message.content().length()>6000) throw new AssistantException(HttpStatus.BAD_REQUEST,"This reply is too long for live speech. Use standard read-aloud.");
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(avatars.speech(message.content()));
    }
}
