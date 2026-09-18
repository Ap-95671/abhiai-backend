package com.abhiai.abhiai_backend.assistant;
import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.*;
import com.abhiai.abhiai_backend.security.JwtPrincipal;

@RestController
@RequestMapping("/api/v1/assistant")
public class AssistantToolsController {
    public record ToolRequest(@NotNull UUID conversationId,@NotBlank @Size(max=48) String name,
        @NotNull @Size(max=1) Map<@Size(max=20) String,@NotNull @Size(max=6000) String> arguments,
        @Valid AssistantPageContext context) {}
    private final AssistantToolRegistry tools;
    private final AssistantPolicy policy;
    private final AssistantConversationService conversations;
    public AssistantToolsController(AssistantToolRegistry tools,AssistantPolicy policy,AssistantConversationService conversations) {
        this.tools=tools;this.policy=policy;this.conversations=conversations;
    }
    @PostMapping("/tools")
    public ResponseEntity<AssistantToolRegistry.Result> execute(@AuthenticationPrincipal JwtPrincipal principal,@Valid @RequestBody ToolRequest request) {
        policy.request(principal.userId());
        conversations.history(principal.userId(),request.conversationId());
        AssistantToolRegistry.validate(request.name(),request.arguments());
        try { return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(tools.execute(principal.userId(),request.name(),request.arguments(),request.context())); }
        catch(AssistantException error) { throw error; }
        catch(RuntimeException error) { throw new AssistantException(HttpStatus.BAD_REQUEST,"That content or tool is unavailable, or you do not have access. You can continue chatting."); }
    }
}
