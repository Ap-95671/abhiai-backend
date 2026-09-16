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
    public enum Permission { READ_ONLY, SAFE_DRAFT, CONFIRMATION_REQUIRED }
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
        new Definition("GET_ASSISTANT_CONTEXT","Read current page and relevant preferences for this request. Call at the beginning of every voice turn.",Permission.READ_ONLY,"query",0,500),
        new Definition("SEARCH_ABHIAI","Search accessible AbhiAI posts. Query must be the search topic only.",Permission.READ_ONLY,"query",2,100),
        new Definition("GET_CURRENT_NEWS_ARTICLE","Read the current news article metadata; full reporting is unavailable.",Permission.READ_ONLY,null,0,0),
        new Definition("GET_CURRENT_POST","Read the currently viewed post.",Permission.READ_ONLY,null,0,0),
        new Definition("GET_PROFILE_CONTEXT","Read current profile and a sample of accessible posts.",Permission.READ_ONLY,null,0,0),
        new Definition("GET_CONVERSATION_CONTEXT","Read a bounded excerpt of the current owned AI chat.",Permission.READ_ONLY,null,0,0),
        new Definition("GET_DOCUMENT_CONTEXT","Read an approved document excerpt from the existing extraction pipeline.",Permission.READ_ONLY,null,0,0),
        new Definition("GET_SAVED_CONTENT","Find a topic within the latest 50 accessible saved posts; empty query lists recent saves.",Permission.READ_ONLY,"query",0,100),
        new Definition("CREATE_POST_DRAFT","Prepare complete post text (maximum 1000 characters) for user review, never publish. Use content from earlier tool results if needed.",Permission.SAFE_DRAFT,"text",1,1000),
        new Definition("PROPOSE_MEMORY","Only when explicitly asked to remember: propose a non-sensitive preference for review. No memory is saved until Save memory is clicked.",Permission.SAFE_DRAFT,"text",1,500),
        new Definition("SET_EXPRESSION","Set one restrained expression: neutral, happy, curious, thinking, excited, supportive, confused, serious. No visible spoken text.",Permission.READ_ONLY,"expression",1,20)
    );
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
        data.put("page",context.safeResolve(userId,page));
        try { data.put("preferences",memory.relevant(userId,query)); }
        catch(RuntimeException ignored) { data.put("memoryNotice","Saved preferences are temporarily unavailable. Continue normally."); }
        return data;
    }
    @Transactional(readOnly=true, propagation=org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public Result execute(UUID userId,String name,Map<String,String> args,AssistantPageContext page) {
        long start=System.nanoTime(); boolean success=false;
        try {
            validate(name,args);
            Result result=switch(name) {
                case "SEARCH_ABHIAI" -> result(name,"cards","Search results","Accessible posts matching your topic.",
                    search.searchPosts(userId,args.get("query"),PageRequest.of(0,5)).content().stream()
                    .filter(p->viewable(userId,p.id())).map(AssistantContextService::post).toList());
                case "GET_SAVED_CONTENT" -> result(name,"cards","Saved posts","Matches in your latest 50 accessible saves.",
                    bookmarks.getBookmarks(userId,PageRequest.of(0,50)).content().stream().map(b->b.post())
                    .filter(p->viewable(userId,p.id()))
                    .filter(p->(p.textContent()==null?"":p.textContent()).toLowerCase(Locale.ROOT).contains(args.get("query").toLowerCase(Locale.ROOT)))
                    .limit(5).map(AssistantContextService::post).toList());
                case "CREATE_POST_DRAFT" -> new Result(name,"draft","Post draft","Review and edit before publishing.",List.of(),args.get("text"),null,"happy");
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
    private boolean viewable(UUID userId,UUID postId) {
        try { access.findViewablePost(userId,postId); return true; } catch(RuntimeException ignored) { return false; }
    }
    private static Result result(String name,String kind,String title,String text,List<Map<String,Object>> cards) {
        return new Result(name,kind,title,text,cards,null,null,cards.isEmpty()?"curious":"neutral");
    }
}
