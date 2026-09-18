package com.abhiai.abhiai_backend.assistant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.validation.constraints.*;
import com.abhiai.abhiai_backend.repository.UserRepository;
@Service
public class AssistantPreferencesService {
    public record Settings(@NotNull @Pattern(regexp="STANDARD|FRIENDLY|TUTOR|PROFESSIONAL|CREATIVE") String mode,
        boolean pageContext, boolean agentActions, boolean proactive,
        @NotNull @Size(max=80) @Pattern(regexp="[\\p{L}0-9 _.-]*") String projectKey, boolean fallbackAllowed) {}
    private final AssistantPreferencesRepository repository;
    private final UserRepository users;
    public AssistantPreferencesService(AssistantPreferencesRepository repository,UserRepository users) { this.repository=repository;this.users=users; }
    @Transactional(readOnly=true) public Settings get(UUID user) {
        var p=repository.findById(user).orElseGet(()->new AssistantPreferences(user));
        return new Settings(p.mode,p.pageContext,p.agentActions,p.proactive,p.projectKey,p.fallbackAllowed);
    }
    @Transactional public Settings update(UUID user,Settings value) {
        users.lockAssistantOwner(user).orElseThrow();
        var p=repository.findById(user).orElseGet(()->new AssistantPreferences(user));
        p.mode=value.mode();p.pageContext=value.pageContext();p.agentActions=value.agentActions();p.proactive=value.proactive();
        p.projectKey=value.projectKey().trim();p.fallbackAllowed=value.fallbackAllowed();repository.save(p);return get(user);
    }
    public static String style(String mode) {
        return switch(mode) { case "FRIENDLY" -> "Use a warm conversational tone.";
            case "TUTOR" -> "Teach step by step with examples and a brief understanding check.";
            case "PROFESSIONAL" -> "Use concise, formal, actionable explanations.";
            case "CREATIVE" -> "Offer varied ideas, clearly separating speculation from facts.";
            default -> "Be clear, balanced and concise."; };
    }
}
