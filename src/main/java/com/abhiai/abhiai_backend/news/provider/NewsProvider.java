package com.abhiai.abhiai_backend.news.provider;

import java.util.Optional;

import com.abhiai.abhiai_backend.news.model.NewsArticle;
import com.abhiai.abhiai_backend.news.model.NewsQuery;

public interface NewsProvider {
    String providerName();
    boolean configured();
    NewsProviderPage fetch(NewsQuery query, String cursor);
    Optional<NewsArticle> fetchById(String articleId);
}
