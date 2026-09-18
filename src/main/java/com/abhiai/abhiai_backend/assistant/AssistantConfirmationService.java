package com.abhiai.abhiai_backend.assistant;

import java.util.*;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import com.abhiai.abhiai_backend.service.*;
import com.abhiai.abhiai_backend.dto.post.PostResponse;
import tools.jackson.databind.ObjectMapper;

/** Exact server-owned payloads. No model-supplied authorization or execution payload after review. */
@Service
public class AssistantConfirmationService {
    public record Review(UUID id,String actionType,String label,String exactPayload,String payloadHash,Instant expiresAt,String status) {}
    private final PendingAssistantActionRepository actions;
    private final AssistantPreferencesService preferences;
    private final PostAccessService access;
    private final PostBookmarkService bookmarks;
    private final ObjectMapper mapper;
    public AssistantConfirmationService(PendingAssistantActionRepository actions,AssistantPreferencesService preferences,
        PostAccessService access,PostBookmarkService bookmarks,ObjectMapper mapper) {
        this.actions=actions;this.preferences=preferences;this.access=access;this.bookmarks=bookmarks;this.mapper=mapper;
    }
    public Review prepare(UUID user,UUID task,String postId) {
        requireActions(user);
        var post=PostResponse.from(access.findViewablePost(user,UUID.fromString(postId)));
        var action=new PendingAssistantAction();action.id=UUID.randomUUID();action.userId=user;action.taskId=task;
        action.actionType="SAVE_POST";
        action.exactPayload=mapper.writeValueAsString(Map.of("postId",post.id().toString(),"author",post.author().username(),
            "text",AssistantContextService.limited(post.textContent(),3000)));
        action.payloadHash=hash(action.exactPayload);action.resourceHash=resource(user,postId);
        action.status="PENDING";action.createdAt=Instant.now();action.expiresAt=action.createdAt.plusSeconds(300);
        actions.save(action);return review(action);
    }
    @Transactional public Review approve(UUID user,UUID task,UUID id,String reviewedHash) {
        requireActions(user);
        var a=actions.lock(id,user).filter(v->v.taskId.equals(task)).orElseThrow(()->invalid("Action unavailable."));
        if(!a.status.equals("PENDING") || !a.expiresAt.isAfter(Instant.now())) throw invalid("Confirmation expired or already used. Resume to review a new action.");
        if(!a.payloadHash.equals(reviewedHash) || !a.payloadHash.equals(hash(a.exactPayload))) throw invalid("The reviewed action does not match.");
        String postId=mapper.readTree(a.exactPayload).path("postId").asString();
        if(!a.resourceHash.equals(resource(user,postId))) throw invalid("This post changed. Resume to review it again.");
        if(!bookmarks.getStatus(user,UUID.fromString(postId)).bookmarked()) bookmarks.bookmark(user,UUID.fromString(postId));
        a.status="EXECUTED";return review(actions.save(a));
    }
    @Transactional public void invalidate(UUID task) {
        actions.findByTaskId(task).stream().filter(a->a.status.equals("PENDING")).forEach(a->{a.status="INVALIDATED";actions.save(a);});
    }
    private String resource(UUID user,String id) {
        var p=access.findViewablePost(user,UUID.fromString(id));
        return hash(p.getId()+":"+p.getTextContent()+":"+p.getVisibility()+":"+p.getUpdatedAt());
    }
    private void requireActions(UUID user) {
        if(!preferences.get(user).agentActions()) throw new AssistantException(HttpStatus.FORBIDDEN,"Enable Agent Actions in Assistant Privacy to review actions.");
    }
    static String hash(String value) {
        try {return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}
        catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}
    }
    private Review review(PendingAssistantAction a) {return new Review(a.id,a.actionType,"Save this post to your saved posts?",a.exactPayload,a.payloadHash,a.expiresAt,a.status);}
    private AssistantException invalid(String message){return new AssistantException(HttpStatus.CONFLICT,message);}
}
