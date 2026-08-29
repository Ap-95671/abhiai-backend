package com.abhiai.abhiai_backend.news.dto;

import java.time.Instant;
import java.util.List;

import com.abhiai.abhiai_backend.news.model.NewsArticle;
import com.abhiai.abhiai_backend.news.model.NewsSource;

public record NewsArticleResponse(
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

    public static NewsArticleResponse from(NewsArticle article) {
        return new NewsArticleResponse(
                article.id(), article.title(), article.description(), article.sourceName(), article.sourceUrl(),
                article.articleUrl(), article.imageUrl(), article.publishedAt(), article.category(), article.country(),
                article.region(), article.language(), article.author(), article.provider(), article.relatedStoryCount(),
                article.sources());
    }
}
