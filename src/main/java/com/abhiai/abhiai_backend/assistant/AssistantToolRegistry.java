package com.abhiai.abhiai_backend.assistant;

import java.util.*;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import com.abhiai.abhiai_backend.service.*;
import com.abhiai.abhiai_backend.entity.MemoryCategory;

/** Application adapter registry. Reuses domain services; it cannot publish, delete, message or write memories. */
@Service
public class AssistantToolRegistry {
    public enum Permission { READ_ONLY, SAFE_DRAFT, SAFE_LOCAL_ACTION, CONFIRMATION_REQUIRED, HIGH_RISK }
    public record Definition(String name, String description, Permission permission, String argument, int min, int max) {
        public Map<String,Object> declaration() {
            Map<String,Object> props=new LinkedHashMap<>();
            if(argument!=null) props.put(argument,Map.of("type","STRING","description",description));
            return Map.of("name",name,"description",description,"parameters",Map.of("type","OBJECT","properties",props,
                "required",argument==null?List.of():List.of(argument)));
        }
    }
    public record Result(String tool, String kind, String title, String text, List<Map<String,Object>> cards,
                         String draft, String memoryCategory, String expression) {}
    public static final List<String> EXPRESSIONS=List.of("neutral","happy","curious","thinking","excited","supportive","confused","serious");
    public static final List<Definition> DEFINITIONS=List.of(
        new Definition("SEARCH_NEWS","Search recent news metadata. Results include dates; never claim all results are from today.",Permission.READ_ONLY,"query",2,100),
        new Definition("GET_NEWS_ARTICLE","Retrieve news metadata using an ID from search results.",Permission.READ_ONLY,"id",1,160),
        new Definition("SEARCH_CONVERSATIONS","Search your owned AI chats by topic.",Permission.READ_ONLY,"query",2,100),
        new Definition("GET_CONVERSATION","Read a bounded excerpt of an owned conversation using its ID.",Permission.READ_ONLY,"id",36,36),
        new Definition("SEARCH_DOCUMENTS","Find owned uploaded documents by topic; excerpts require processing consent.",Permission.READ_ONLY,"query",2,100),
        new Definition("GET_DOCUMENT","Read an approved document using conversationId/attachmentId from search results. Requires document consent.",Permission.READ_ONLY,"reference",73,73),
        new Definition("GET_CREATOR_ANALYTICS","Read your real creator analytics for the last 30 days.",Permission.READ_ONLY,null,0,0),
        new Definition("OPEN_ROUTE","Offer an internal navigation link: news, saved, social, chat, or creator.",Permission.SAFE_LOCAL_ACTION,"destination",4,20),
        new Definition("SAVE_POST","Propose saving a post ID. Only the task confirmation endpoint can execute this action after explicit review.",Permission.CONFIRMATION_REQUIRED,"id",36,36),
        new Definition("CREATE_STUDY_PLAN","Prepare a study plan grounded in previous results.",Permission.SAFE_LOCAL_ACTION,"text",1,6000),
        new Definition("CREATE_QUIZ","Prepare a quiz and answers from previous results.",Permission.SAFE_LOCAL_ACTION,"text",1,6000),
        new Definition("CREATE_SUMMARY","Prepare a summary or comparison from previous results.",Permission.SAFE_LOCAL_ACTION,"text",1,6000),
        new Definition("CREATE_MESSAGE_DRAFT","Prepare message text for review; never send it.",Permission.SAFE_LOCAL_ACTION,"text",1,3000),
        new Definition("GET_ASSISTANT_CONTEXT","Read current page and relevant preferences for this request. Call at the beginning of every voice turn.",Permission.READ_ONLY,"query",0,500),
        new Definition("SEARCH_ABHIAI","Search accessible AbhiAI posts. Query must be the search topic only.",Permission.READ_ONLY,"query",2,100),
        new Definition("GET_CURRENT_NEWS_ARTICLE","Read the current news article metadata; full reporting is unavailable.",Permission.READ_ONLY,null,0,0),
        new Definition("GET_CURRENT_POST","Read the currently viewed post.",Permission.READ_ONLY,null,0,0),
        new Definition("GET_PROFILE_CONTEXT","Read current profile and a sample of accessible posts.",Permission.READ_ONLY,null,0,0),
        new Definition("GET_CONVERSATION_CONTEXT","Read a bounded excerpt of the current owned AI chat.",Permission.READ_ONLY,null,0,0),
        new Definition("GET_DOCUMENT_CONTEXT","Read an approved document excerpt from the existing extraction pipeline.",Permission.READ_ONLY,null,0,0),
        new Definition("GET_SAVED_CONTENT","Find a topic within the latest 50 accessible saved posts; empty query lists recent saves.",Permission.READ_ONLY,"query",0,100),
        new Definition("CREATE_POST_DRAFT","Prepare complete post text (maximum 1000 characters) for user review, never publish. Use content from earlier tool results if needed.",Permission.SAFE_DRAFT,"text",1,1000),
        new Definition("PROPOSE_FORGET_MEMORY","Find explicitly saved memories matching the user's request to forget. Ask for clarification if ambiguous; deletion requires the user's Forget click.",Permission.SAFE_LOCAL_ACTION,"query",2,100),
        new Definition("PROPOSE_MEMORY","Only when explicitly asked to remember: propose a non-sensitive preference for review. No memory is saved until Save memory is clicked.",Permission.SAFE_DRAFT,"text",1,500),
        new Definition("SET_EXPRESSION","Set one restrained expression: neutral, happy, curious, thinking, excited, supportive, confused, serious. No visible spoken text.",Permission.READ_ONLY,"expression",1,20)
    );
    @org.springframework.beans.factory.annotation.Autowired private com.abhiai.abhiai_backend.news.service.NewsService news;
    @org.springframework.beans.factory.annotation.Autowired private com.abhiai.abhiai_backend.repository.ConversationRepository conversations;
    @org.springframework.beans.factory.annotation.Autowired private com.abhiai.abhiai_backend.repository.ConversationAttachmentRepository documents;
    @org.springframework.beans.factory.annotation.Autowired private ConversationAttachmentService attachments;
    @org.springframework.beans.factory.annotation.Autowired private CreatorAnalyticsService analytics;
    @org.springframework.beans.factory.annotation.Autowired private AssistantPreferencesService preferences;
    private final AssistantContextService context;
    private final SearchService search;
    private final PostBookmarkService bookmarks;
    private final PostAccessService access;
    private final AiMemoryService memory;
    public AssistantToolRegistry(AssistantContextService context,SearchService search,PostBookmarkService bookmarks,
                                 PostAccessService access,AiMemoryService memory) {
        this.context=context;this.search=search;this.bookmarks=bookmarks;this.access=access;this.memory=memory;
    }
    public static List<Map<String,Object>> declarations() { return DEFINITIONS.stream().map(Definition::declaration).toList(); }
    public static Definition validate(String name, Map<String,String> args) {
        var def=DEFINITIONS.stream().filter(d->d.name().equals(name)).findFirst()
            .orElseThrow(()->new AssistantException(HttpStatus.BAD_REQUEST,"Unsupported assistant tool."));
        if(args==null || args.size()>(def.argument()==null?0:1) || args.keySet().stream().anyMatch(k->!k.equals(def.argument()))) throw invalid();
        if(def.argument()!=null) {
            String value=args.get(def.argument());
            if(value==null || value.trim().length()<def.min() || value.length()>def.max()) throw invalid();
        }
        if(name.equals("SET_EXPRESSION") && !EXPRESSIONS.contains(args.get("expression"))) throw invalid();
        return def;
    }
    private static AssistantException invalid() { return new AssistantException(HttpStatus.BAD_REQUEST,"Invalid assistant tool arguments."); }
    public Map<String,Object> environment(UUID userId, AssistantPageContext page, String query) {
        var data=new LinkedHashMap<String,Object>();
        var settings=preferences==null?null:preferences.get(userId);
        if(settings!=null && !settings.pageContext()) page=null;
        data.put("page",context.safeResolve(userId,page));
        if(settings!=null) data.put("presentationMode",settings.mode());
        try { data.put("preferences",memory.relevant(userId,query,preferences==null?"":preferences.get(userId).projectKey(),null,null)); }
        catch(RuntimeException ignored) { data.put("memoryNotice","Saved preferences are temporarily unavailable. Continue normally."); }
        return data;
    }
    @Transactional(readOnly=true, propagation=org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public Result execute(UUID userId,String name,Map<String,String> args,AssistantPageContext page) {
        long start=System.nanoTime(); boolean success=false;
        try {
            var definition=validate(name,args);
            if(definition.permission()==Permission.CONFIRMATION_REQUIRED || definition.permission()==Permission.HIGH_RISK)
                throw new AssistantException(HttpStatus.FORBIDDEN,"This action requires exact task confirmation.");
            if(preferences!=null && !preferences.get(userId).pageContext()) page=null;
            Result result=switch(name) {
                case "SEARCH_NEWS" -> result(name,"cards","News results","Publisher metadata, not full reporting. Check publication dates.",
                    news.list(null,null,null,args.get("query"),0,3,false).content().stream().map(a->newsCard(a)).toList());
                case "GET_NEWS_ARTICLE" -> result(name,"cards","News article","Publisher metadata only.",List.of(newsCard(news.get(args.get("id")))));
                case "SEARCH_CONVERSATIONS" -> result(name,"cards","Your conversations","Owned conversations matching your topic.",
                    conversations.searchOwnedConversations(userId,args.get("query"),PageRequest.of(0,5)).stream()
                        .map(c->Map.<String,Object>of("id",c.getId().toString(),"title",c.getTitle(),"entityType","conversation")).toList());
                case "GET_CONVERSATION" -> result(name,"cards","Conversation excerpt","Recent messages only.",List.of(context.resolve(userId,
                    new AssistantPageContext("conversation","/",args.get("id"),null,null,null,false))));
                case "SEARCH_DOCUMENTS" -> result(name,"cards","Your documents","Choose a document and approve external processing before reading.",
                    documents.searchOwned(userId,args.get("query"),PageRequest.of(0,5)).stream().map(a->Map.<String,Object>of(
                        "id",a.getId().toString(),"reference",a.getConversation().getId()+"/"+a.getId(),
                        "title",a.getMediaAsset().getOriginalFilename(),"entityType","document")).toList());
                case "GET_DOCUMENT" -> {
                    String[] ids=args.get("reference").split("/");
                    if(page==null || !page.externalProcessingAllowed() || !page.pageType().equals("document") || ids.length!=2
                        || !ids[0].equals(page.parentId()) || !ids[1].equals(page.entityId()))
                        throw new AssistantException(HttpStatus.FORBIDDEN,"Open this document and approve its processing first.");
                    yield result(name,"cards","Document excerpt","Relevant extracted text.",List.of(attachments.assistantContext(userId,UUID.fromString(ids[0]),UUID.fromString(ids[1]),page.currentPage(),page.currentSection())));
                }
                case "GET_CREATOR_ANALYTICS" -> {
                    var a=analytics.dashboard(userId,30);
                    yield result(name,"cards","Your creator analytics","Actual last 30 days.",List.of(Map.of("title","30-day performance","text",
                        "Impressions: "+a.impressions()+"; engagements: "+a.engagements()+"; follower growth: "+a.followerGrowth())));
                }
                case "OPEN_ROUTE" -> {
                    var routes=Map.of("news","/news","saved","/social?view=bookmarks","social","/social","chat","/","creator","/social?view=creator");
                    String href=routes.get(args.get("destination"));if(href==null) throw invalid();
                    yield result(name,"cards","Open "+args.get("destination"),"Select the link to navigate.",List.of(Map.of("title","Open "+args.get("destination"),"href",href)));
                }
                case "CREATE_STUDY_PLAN", "CREATE_QUIZ", "CREATE_SUMMARY", "CREATE_MESSAGE_DRAFT" ->
                    new Result(name,"artifact",name.substring(7).replace('_',' '),"Prepared for your review; no content was sent or published.",List.of(),args.get("text"),null,"happy");
                case "SEARCH_ABHIAI" -> result(name,"cards","Search results","Accessible posts matching your topic.",
                    search.searchPosts(userId,args.get("query"),PageRequest.of(0,5)).content().stream()
                    .filter(p->viewable(userId,p.id())).map(AssistantContextService::post).toList());
                case "GET_SAVED_CONTENT" -> result(name,"cards","Saved posts","Matches in your latest 50 accessible saves.",
                    bookmarks.getBookmarks(userId,PageRequest.of(0,50)).content().stream().map(b->b.post())
                    .filter(p->viewable(userId,p.id()))
                    .filter(p->(p.textContent()==null?"":p.textContent()).toLowerCase(Locale.ROOT).contains(args.get("query").toLowerCase(Locale.ROOT)))
                    .limit(5).map(AssistantContextService::post).toList());
                case "CREATE_POST_DRAFT" -> new Result(name,"draft","Post draft","Review and edit before publishing.",List.of(),args.get("text"),null,"happy");
                case "PROPOSE_FORGET_MEMORY" -> result(name,"forget","Review memories to forget","Choose exactly which saved item to remove.",
                    memory.settings(userId).memories().stream().filter(m->m.content().toLowerCase(Locale.ROOT).contains(args.get("query").toLowerCase(Locale.ROOT)))
                    .limit(5).map(m->Map.<String,Object>of("id",m.id().toString(),"title",m.content(),"text",m.scope()+" "+m.scopeKey())).toList());
                case "PROPOSE_MEMORY" -> {
                    AiMemoryService.validatePrivacy(args.get("text"));
                    yield new Result(name,"memory","Suggested memory","Not saved. Review and click Save memory to confirm.",List.of(),args.get("text"),MemoryCategory.PREFERENCE.name(),"supportive");
                }
                case "SET_EXPRESSION" -> new Result(name,"expression","","",List.of(),null,null,args.get("expression"));
                case "GET_ASSISTANT_CONTEXT" -> result(name,"context","Current context","",List.of(environment(userId,page,args.get("query"))));
                default -> {
                    String type=switch(name) { case "GET_CURRENT_NEWS_ARTICLE"->"news";case "GET_CURRENT_POST"->"post";
                        case "GET_PROFILE_CONTEXT"->"profile";case "GET_CONVERSATION_CONTEXT"->"conversation";default->"document";};
                    if(page==null || !page.pageType().equals(type)) yield result(name,"notice","Context unavailable","Open the content and enable page context first.",List.of());
                    var value=context.resolve(userId,page);
                    yield result(name,"cards","Current "+type,"Only accessible context is included.",List.of(value));
                }
            };
            success=true; return result;
        } finally {
            LoggerFactory.getLogger(getClass()).info("assistant_tool name={} user={} success={} latencyMs={}",
                DEFINITIONS.stream().anyMatch(d->d.name().equals(name))?name:"UNKNOWN",userId,success,(System.nanoTime()-start)/1_000_000);
        }
    }
    @Transactional(readOnly=true)
    public void revalidate(UUID user,Result result,AssistantPageContext page) {
        if(result==null)return;
        for(var card:result.cards()) {
            String type=String.valueOf(card.getOrDefault("entityType",""));
            String id=String.valueOf(card.getOrDefault("id",""));
            if(type.equals("post") || String.valueOf(card.getOrDefault("href","")).startsWith("/social#post-")) access.findViewablePost(user,UUID.fromString(id));
            if(type.equals("conversation")) conversations.findByIdAndUserId(UUID.fromString(id),user).orElseThrow();
            if(type.equals("document")) {
                var a=documents.findById(UUID.fromString(id)).orElseThrow();
                conversations.findByIdAndUserId(a.getConversation().getId(),user).orElseThrow();
            }
        }
    }
    private static Map<String,Object> newsCard(com.abhiai.abhiai_backend.news.dto.NewsArticleResponse a) {
        return Map.of("id",a.id(),"entityType","newsArticle","title",a.title(),"text",AssistantContextService.limited(a.description(),2500),
            "source",a.sourceName()==null?"Unknown":a.sourceName(),"publishedAt",a.publishedAt()==null?"Unknown":a.publishedAt().toString(),
            "href","/news#"+java.net.URLEncoder.encode(a.id(),java.nio.charset.StandardCharsets.UTF_8));
    }
    private boolean viewable(UUID userId,UUID postId) {
        try { access.findViewablePost(userId,postId); return true; } catch(RuntimeException ignored) { return false; }
    }
    private static Result result(String name,String kind,String title,String text,List<Map<String,Object>> cards) {
        return new Result(name,kind,title,text,cards,null,null,cards.isEmpty()?"curious":"neutral");
    }
}
