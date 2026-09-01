package com.abhiai.abhiai_backend.ai.tool;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class AiToolRegistry {

    private final WebSearchTool webSearch;

    public AiToolRegistry(WebSearchTool webSearch) {
        this.webSearch = webSearch;
    }

    public String augmentPrompt(String prompt, boolean webSearchAllowed) {
        return augmentPromptWithSources(prompt, webSearchAllowed).prompt();
    }

    public AugmentedPrompt augmentPromptWithSources(String prompt, boolean webSearchAllowed) {
        if (!webSearchAllowed) {
            return new AugmentedPrompt(prompt, List.of());
        }
        WebSearchResult result = webSearch.search(prompt);
        return new AugmentedPrompt(
                prompt + "\n\n" + result.context()
                        + "\nUse these results only when relevant. Cite factual claims with the numbered source URLs and do not invent sources.",
                result.sources());
    }

    public boolean webSearchConfigured() {
        return webSearch.configured();
    }

    public record AugmentedPrompt(String prompt, List<WebSearchSource> sources) {
        public AugmentedPrompt {
            sources = List.copyOf(sources);
        }
    }
}
