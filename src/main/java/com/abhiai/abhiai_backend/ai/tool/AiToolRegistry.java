package com.abhiai.abhiai_backend.ai.tool;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import com.abhiai.abhiai_backend.ai.orchestration.ExecutionPlanner;
import com.abhiai.abhiai_backend.config.AiOrchestrationProperties;
import com.abhiai.abhiai_backend.config.WebSearchProperties;
import com.abhiai.abhiai_backend.exception.*;

/** Existing registry is the controlled tool orchestrator. Only registered, authorized operations can execute. */
@Service
public class AiToolRegistry {
    private final WebSearchTool webSearch;
    private final ExecutionPlanner planner;
    private final WebSearchProperties properties;
    public AiToolRegistry(WebSearchTool webSearch) { this(webSearch, new ExecutionPlanner(new AiOrchestrationProperties()), new WebSearchProperties()); }
    @Autowired
    public AiToolRegistry(WebSearchTool webSearch, ExecutionPlanner planner, WebSearchProperties properties) {
        this.webSearch = webSearch; this.planner = planner; this.properties = properties;
    }
    public String augmentPrompt(String prompt, boolean webSearchAllowed) { return augmentPromptWithSources(prompt, webSearchAllowed).prompt(); }
    public AugmentedPrompt augmentPromptWithSources(String prompt, boolean webSearchAllowed) {
        var plan = planner.planTools(prompt, webSearchAllowed);
        if (!plan.search()) return new AugmentedPrompt(prompt, List.of());
        if (!webSearchAllowed && !properties.isAutoEnabled())
            throw new AiProviderException("Current information requires web search, which is disabled. Enable Web Search to continue.", AiProviderFailureKind.SEARCH_FAILURE);
        long started = System.nanoTime();
        boolean success = false;
        try {
            WebSearchResult result = executeTool("web_search", prompt, true);
            success = true;
            return new AugmentedPrompt(prompt + "\n\n" + result.context(), result.sources());
        } finally {
            org.slf4j.LoggerFactory.getLogger(getClass()).info("ai_tool requestId={} tool=web_search reason={} success={} latencyMs={}",
                    org.slf4j.MDC.get("requestId"), plan.reason(), success, (System.nanoTime()-started)/1_000_000);
        }
    }
    public WebSearchResult executeTool(String name, String input, boolean permitted) {
        if (!permitted || !"web_search".equals(name))
            throw new AiProviderException("This tool operation is not permitted.", AiProviderFailureKind.TOOL_FAILURE);
        if (input == null || input.isBlank() || input.length() > 10000)
            throw new AiProviderException("A valid search query is required.", AiProviderFailureKind.SEARCH_FAILURE);
        // Exactly one controlled call. Models cannot execute this method or request arbitrary URLs.
        return webSearch.search(input);
    }
    public boolean webSearchConfigured() { return webSearch.configured(); }
    public record AugmentedPrompt(String prompt, List<WebSearchSource> sources) {
        public AugmentedPrompt { sources = List.copyOf(sources); }
    }
}
