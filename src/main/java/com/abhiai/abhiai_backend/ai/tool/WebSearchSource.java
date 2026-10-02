package com.abhiai.abhiai_backend.ai.tool;

import java.time.Instant;

/** sourceDate is provider-reported publication OR modification date; never fabricated. */
public record WebSearchSource(String title, String url, String description, String domain, String sourceDate, Instant retrievedAt) {
    public WebSearchSource(String title, String url, String description, String domain) {
        this(title, url, description, domain, null, null);
    }
}
