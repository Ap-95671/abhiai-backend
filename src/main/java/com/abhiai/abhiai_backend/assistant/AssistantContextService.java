package com.abhiai.abhiai_backend.assistant;

import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import com.abhiai.abhiai_backend.service.*;
import com.abhiai.abhiai_backend.repository.*;
import com.abhiai.abhiai_backend.entity.*;
import com.abhiai.abhiai_backend.news.service.NewsService;
import com.abhiai.abhiai_backend.dto.post.PostResponse;

@Service
public class AssistantContextService {
    private final NewsService news;
    private final PostAccessService posts;
    private final UserProfileService profiles;
    private final ProfileContentService profileContent;
    private final ConversationRepository conversations;
    private final MessageRepository messages;
    private final ConversationAttachmentService attachments;
    public AssistantContextService(NewsService news, PostAccessService posts, UserProfileService profiles,
            ProfileContentService profileContent, ConversationRepository conversations, MessageRepository messages,
            ConversationAttachmentService attachments) {
        this.news=news; this.posts=posts; this.profiles=profiles; this.profileContent=profileContent;
        this.conversations=conversations; this.messages=messages; this.attachments=attachments;
    }
    public static String limited(String value, int max) { return value == null ? "" : value.substring(0, Math.min(value.length(), max)); }
    public static Map<String,Object> post(PostResponse p) {
        return Map.of("id", p.id().toString(), "title", "Post by @"+p.author().username(),
                "text", limited(p.textContent(),3000), "href", "/social#post-"+p.id());
    }
    @Transactional(readOnly=true)
    public Map<String,Object> resolve(UUID userId, AssistantPageContext context) {
        if(context==null) return Map.of("available",false,"reason","Page context is disabled or unavailable.");
        Map<String,Object> data = new LinkedHashMap<>();
        data.put("pageType",context.pageType());
        if(context.entityId()!=null && !context.entityId().isBlank()) switch(context.pageType()) {
            case "news" -> {
                var article=news.get(context.entityId());
                data.putAll(Map.of("id",article.id(),"title",limited(article.title(),240),
                    "text",limited(article.description(),4000),"source",limited(article.sourceName(),160),
                    "coverage","Publisher metadata only, not the full article. Do not invent paragraphs.",
                    "href","/news#"+java.net.URLEncoder.encode(article.id(),java.nio.charset.StandardCharsets.UTF_8)));
            }
            case "post" -> data.putAll(post(PostResponse.from(posts.findViewablePost(userId,UUID.fromString(context.entityId())))));
            case "profile" -> {
                var profile=profiles.getByUsername(userId,context.entityId());
                data.putAll(Map.of("title",limited(profile.displayName(),120),"username",profile.username(),
                    "text",limited(profile.bio(),1000),"posts",profileContent.getPosts(userId,profile.username(),PageRequest.of(0,3))
                    .content().stream().map(AssistantContextService::post).toList(),
                    "href","/social?view=profile&username="+profile.username()));
            }
            case "conversation" -> {
                UUID id=UUID.fromString(context.entityId());
                var conversation=conversations.findByIdAndUserId(id,userId).orElseThrow(()->new IllegalArgumentException("Unavailable"));
                data.put("title",limited(conversation.getTitle(),240));
                var history=new ArrayList<>(messages.findByConversationIdOrderByCreatedAtDescIdDesc(id,PageRequest.of(0,6)));
                Collections.reverse(history);
                data.put("messages",history.stream()
                    .filter(m->m.getRole()!=MessageRole.SYSTEM).map(m->Map.of("role",m.getRole().name(),"text",limited(m.getContent(),1000))).toList());
            }
            case "document" -> {
                if(!context.externalProcessingAllowed()) throw new IllegalArgumentException("Document processing not approved");
                data.putAll(attachments.assistantContext(userId,UUID.fromString(context.parentId()),UUID.fromString(context.entityId())));
            }
            default -> { }
        }
        // Selection is only accepted when it occurs in the already-authorized text. No input/DOM scraping.
        String selection=limited(context.selectedText(),2000).trim();
        if(!selection.isEmpty() && containsText(data, selection))
            data.put("selectedText",selection);
        data.put("available",data.containsKey("title"));
        return Collections.unmodifiableMap(data);
    }
    private static boolean containsText(Object value, String selection) {
        if (value instanceof String text) return text.contains(selection);
        if (value instanceof Map<?,?> map) return map.values().stream().anyMatch(v -> containsText(v, selection));
        if (value instanceof List<?> list) return list.stream().anyMatch(v -> containsText(v, selection));
        return false;
    }
    public Map<String,Object> safeResolve(UUID userId, AssistantPageContext context) {
        try { return resolve(userId,context); }
        catch(RuntimeException ignored) { return Map.of("available",false,"reason","This content is unavailable or you do not have access."); }
    }
}
