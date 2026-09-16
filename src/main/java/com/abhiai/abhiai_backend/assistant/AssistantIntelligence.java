package com.abhiai.abhiai_backend.assistant;

import java.util.*;
import java.util.function.Consumer;
import org.springframework.stereotype.Service;
import com.abhiai.abhiai_backend.ai.*;
import com.abhiai.abhiai_backend.ai.gemini.GeminiProvider;
import com.abhiai.abhiai_backend.entity.MessageRole;
import tools.jackson.databind.ObjectMapper;

/** Bounded structured planning followed by the existing text stream. Planning failure preserves ordinary chat. */
@Service
public class AssistantIntelligence {
    private final GeminiProvider gemini;
    private final AssistantToolRegistry tools;
    private final ObjectMapper mapper;
    public AssistantIntelligence(GeminiProvider gemini,AssistantToolRegistry tools,ObjectMapper mapper) {
        this.gemini=gemini;this.tools=tools;this.mapper=mapper;
    }
    public List<AiChatMessage> prepare(UUID userId,List<AiChatMessage> history,String query,
                                     AssistantPageContext page,Consumer<Object> events) {
        var context=tools.environment(userId,page,query);
        var data=new AiChatMessage(MessageRole.USER,mapper.writeValueAsString(Map.of("UNTRUSTED_CURRENT_CONTEXT",context)));
        var messages=new ArrayList<>(history);
        messages.add(Math.max(0,messages.size()-1),data);
        if(context.containsKey("memoryNotice")) events.accept(Map.of("notice",context.get("memoryNotice")));
        events.accept(Map.of("status","Considering your request…"));
        try {
            var planning=new ArrayList<AiChatMessage>();
            planning.add(new AiChatMessage(MessageRole.SYSTEM,AssistantPersonality.INSTRUCTIONS+"\n"
                +"Select zero to three tools only for the user's request, plus a suitable expression. Do not explain your reasoning. "
                +"Do not call GET_ASSISTANT_CONTEXT or SET_EXPRESSION here; context and expression are already provided. "
                +"Tool definitions: "+mapper.writeValueAsString(AssistantToolRegistry.declarations())));
            planning.addAll(messages);
            var plan=mapper.readTree(gemini.generateStructured(new AiChatRequest(planning),schema()));
            String expression=plan.path("expression").asString("neutral");
            if(AssistantToolRegistry.EXPRESSIONS.contains(expression)) events.accept(Map.of("expression",expression));
            var calls=plan.path("tools");
            if(!calls.isArray() || calls.size()>3) throw new IllegalArgumentException();
            for(var call:calls) {
                if(Thread.currentThread().isInterrupted()) throw new IllegalStateException("Cancelled");
                String name=call.path("name").asString();
                var def=AssistantToolRegistry.DEFINITIONS.stream().filter(d->d.name().equals(name)).findFirst().orElseThrow();
                if(name.equals("GET_ASSISTANT_CONTEXT") || name.equals("SET_EXPRESSION")) continue;
                Map<String,String> args=def.argument()==null?Map.of():Map.of(def.argument(),call.path("value").asString());
                events.accept(Map.of("status",status(name)));
                try {
                    var result=tools.execute(userId,name,args,page);
                    events.accept(Map.of("result",result));
                    messages.add(messages.size()-1,new AiChatMessage(MessageRole.USER,
                        mapper.writeValueAsString(Map.of("UNTRUSTED_TOOL_RESULT",result))));
                } catch(RuntimeException failure) {
                    if(Thread.currentThread().isInterrupted()) throw failure;
                    String notice="I couldn't complete that tool request. The content may be unavailable or inaccessible.";
                    events.accept(Map.of("notice",notice));
                    messages.add(messages.size()-1,new AiChatMessage(MessageRole.USER,mapper.writeValueAsString(Map.of("tool",name,"error",notice))));
                }
            }
        } catch(RuntimeException failure) {
            if(Thread.currentThread().isInterrupted()) throw failure;
            events.accept(Map.of("notice","Tools are temporarily unavailable. You can still chat."));
            messages.add(messages.size()-1,new AiChatMessage(MessageRole.USER,"{\"toolStatus\":\"unavailable; no action was performed\"}"));
        }
        messages.add(0,new AiChatMessage(MessageRole.SYSTEM,
            "This is the FINAL TEXT RESPONSE stage. The application has already executed the selected tools. "
            + "No function calls are available in this stage. Answer the current user directly using the supplied results. "
            + "Do not request tools again, emit function syntax, or describe expression metadata. "
            + "If a result contains a draft, include its text. Say when context or tools are unavailable."));
        events.accept(Map.of("status","Writing your response…"));
        return messages;
    }
    private static String status(String name) {
        return switch(name) { case "SEARCH_ABHIAI"->"Searching AbhiAI…"; case "CREATE_POST_DRAFT"->"Preparing your draft…";
            case "GET_SAVED_CONTENT"->"Finding saved posts…";case "PROPOSE_MEMORY"->"Preparing a memory for review…";default->"Reading current content…";};
    }
    static Map<String,Object> schema() {
        var call=Map.of("type","object","properties",Map.of("name",Map.of("type","string","enum",AssistantToolRegistry.DEFINITIONS.stream().map(d->d.name()).toList()),
            "value",Map.of("type","string")),"required",List.of("name","value"),"additionalProperties",false);
        return Map.of("type","object","properties",Map.of("expression",Map.of("type","string","enum",AssistantToolRegistry.EXPRESSIONS),
            "tools",Map.of("type","array","items",call,"maxItems",3)),"required",List.of("expression","tools"),"additionalProperties",false);
    }
}
