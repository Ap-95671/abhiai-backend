package com.abhiai.abhiai_backend.assistant;
import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.abhiai.abhiai_backend.security.JwtPrincipal;
@RestController @RequestMapping("/api/v1/assistant")
public class AgentController {
    public record Start(@NotNull UUID conversationId,@NotBlank @Size(max=2000) String goal,@Valid AssistantPageContext context,@NotNull UUID sessionId) {}
    public record Approval(@NotNull UUID actionId,@NotBlank @Pattern(regexp="[a-f0-9]{64}") String payloadHash,@NotNull UUID sessionId) {}
    public record Resume(@NotNull UUID sessionId) {}
    private final AgentOrchestrator agent;
    private final AssistantPreferencesService preferences;
    private final AssistantPolicy policy;
    public AgentController(AgentOrchestrator agent,AssistantPreferencesService preferences,AssistantPolicy policy){this.agent=agent;this.preferences=preferences;this.policy=policy;}
    private <T> ResponseEntity<T> response(T body){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);}
    @GetMapping("/preferences") public ResponseEntity<AssistantPreferencesService.Settings> preferences(@AuthenticationPrincipal JwtPrincipal p){policy.requireEnabled();return response(preferences.get(p.userId()));}
    @PutMapping("/preferences") public ResponseEntity<AssistantPreferencesService.Settings> preferences(@AuthenticationPrincipal JwtPrincipal p,@Valid @RequestBody AssistantPreferencesService.Settings body){policy.request(p.userId());var result=preferences.update(p.userId(),body);agent.cancelActive(p.userId());return response(result);}
    @GetMapping("/tasks") public ResponseEntity<List<AgentOrchestrator.View>> history(@AuthenticationPrincipal JwtPrincipal p){policy.requireEnabled();return response(agent.history(p.userId()));}
    @GetMapping("/tasks/{id}") public ResponseEntity<AgentOrchestrator.View> get(@AuthenticationPrincipal JwtPrincipal p,@PathVariable UUID id){policy.requireEnabled();return response(agent.get(p.userId(),id));}
    @PostMapping("/tasks") public ResponseEntity<AgentOrchestrator.View> create(@AuthenticationPrincipal JwtPrincipal p,@Valid @RequestBody Start body){policy.request(p.userId());return response(agent.create(p.userId(),body.conversationId(),body.goal(),body.context(),body.sessionId()));}
    @PostMapping("/tasks/{id}/advance") public ResponseEntity<AgentOrchestrator.View> advance(@AuthenticationPrincipal JwtPrincipal p,@PathVariable UUID id){policy.request(p.userId());return response(agent.advance(p.userId(),id));}
    @PostMapping("/tasks/{id}/cancel") public ResponseEntity<AgentOrchestrator.View> cancel(@AuthenticationPrincipal JwtPrincipal p,@PathVariable UUID id){policy.requireEnabled();return response(agent.cancel(p.userId(),id));}
    @PostMapping("/tasks/{id}/resume") public ResponseEntity<AgentOrchestrator.View> resume(@AuthenticationPrincipal JwtPrincipal p,@PathVariable UUID id,@Valid @RequestBody Resume body){policy.request(p.userId());return response(agent.resume(p.userId(),id,body.sessionId()));}
    @PostMapping("/tasks/{id}/confirm") public ResponseEntity<AgentOrchestrator.View> confirm(@AuthenticationPrincipal JwtPrincipal p,@PathVariable UUID id,@Valid @RequestBody Approval body){policy.request(p.userId());return response(agent.confirm(p.userId(),id,body.actionId(),body.payloadHash(),body.sessionId()));}
}
