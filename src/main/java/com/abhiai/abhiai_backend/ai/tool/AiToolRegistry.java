package com.abhiai.abhiai_backend.ai.tool;

import org.springframework.stereotype.Service;

@Service
public class AiToolRegistry {

    private final WebSearchTool webSearch;

    public AiToolRegistry(WebSearchTool webSearch) {
        this.webSearch = webSearch;
    }

    public String augmentPrompt(String prompt, boolean webSearchAllowed) {
        if (!webSearchAllowed) {
            return prompt;
        }
        return prompt + "\n\n" + webSearch.execute(prompt)
                + "\nUse these results only when relevant and cite source URLs.";
    }

    public boolean webSearchConfigured() {
        return webSearch.configured();
    }
}
