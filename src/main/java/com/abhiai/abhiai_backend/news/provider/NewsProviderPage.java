package com.abhiai.abhiai_backend.news.provider;

import java.util.List;

import com.abhiai.abhiai_backend.news.model.NewsArticle;

public record NewsProviderPage(List<NewsArticle> articles, String nextCursor, long totalResults) {
    public NewsProviderPage {
        articles = articles == null ? List.of() : List.copyOf(articles);
    }
}
