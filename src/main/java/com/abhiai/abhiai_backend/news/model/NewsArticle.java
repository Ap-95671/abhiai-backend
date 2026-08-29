package com.abhiai.abhiai_backend.news.model;

import java.time.Instant;
import java.util.List;

public record NewsArticle(
        String id,
        String title,
        String description,
        String sourceName,
        String sourceUrl,
        String articleUrl,
        String imageUrl,
        Instant publishedAt,
        String category,
        String country,
        String region,
        String language,
        String author,
        String provider,
        int relatedStoryCount,
        List<NewsSource> sources) {

    public NewsArticle {
        sources = sources == null ? List.of() : List.copyOf(sources);
    }
}
