package com.abhiai.abhiai_backend.news.dto;

import java.time.Instant;
import java.util.List;

public record NewsPageResponse(
        List<NewsArticleResponse> content,
        int page,
        int limit,
        long totalResults,
        boolean hasMore,
        Instant updatedAt,
        boolean stale) {
}
