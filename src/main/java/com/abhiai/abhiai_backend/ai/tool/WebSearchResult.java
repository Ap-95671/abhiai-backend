package com.abhiai.abhiai_backend.ai.tool;

import java.util.List;

public record WebSearchResult(String context, List<WebSearchSource> sources) {

    public WebSearchResult {
        sources = List.copyOf(sources);
    }
}
