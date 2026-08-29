package com.abhiai.abhiai_backend.news.service;

import java.util.Locale;

import org.springframework.stereotype.Service;

import com.abhiai.abhiai_backend.news.cache.NewsCache;
import com.abhiai.abhiai_backend.news.config.NewsProperties;
import com.abhiai.abhiai_backend.news.dto.NewsArticleResponse;
import com.abhiai.abhiai_backend.news.dto.NewsPageResponse;
import com.abhiai.abhiai_backend.news.exception.InvalidNewsQueryException;
import com.abhiai.abhiai_backend.news.exception.NewsArticleNotFoundException;
import com.abhiai.abhiai_backend.news.model.NewsCategory;
import com.abhiai.abhiai_backend.news.model.NewsQuery;
import com.abhiai.abhiai_backend.news.model.NewsRegion;

@Service
public class NewsService {

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 20;
    private final NewsCache cache;
    private final NewsProperties properties;

    public NewsService(NewsCache cache, NewsProperties properties) {
        this.cache = cache;
        this.properties = properties;
    }

    public NewsPageResponse list(
            String category,
            String region,
            String language,
            String query,
            Integer page,
            Integer limit,
            boolean refresh) {
        NewsQuery normalized = normalize(category, region, language, query, page, limit);
        NewsCache.CachedNewsPage result = cache.get(normalized, refresh);
        return new NewsPageResponse(
                result.articles().stream().map(NewsArticleResponse::from).toList(),
                normalized.page(), normalized.limit(), result.totalResults(), result.hasMore(),
                result.updatedAt(), result.stale());
    }

    public NewsArticleResponse get(String articleId) {
        if (articleId == null || articleId.isBlank() || articleId.length() > 160) throw new NewsArticleNotFoundException();
        return cache.find(articleId).map(NewsArticleResponse::from).orElseThrow(NewsArticleNotFoundException::new);
    }

    private NewsQuery normalize(String category, String region, String language, String query, Integer page, Integer limit) {
        int normalizedPage = page == null ? 0 : page;
        int normalizedLimit = limit == null ? DEFAULT_LIMIT : limit;
        if (normalizedPage < 0) throw new InvalidNewsQueryException("News page must be zero or greater");
        if (normalizedLimit < 1 || normalizedLimit > MAX_LIMIT) {
            throw new InvalidNewsQueryException("News limit must be between 1 and " + MAX_LIMIT);
        }
        String normalizedQuery = query == null ? "" : query.trim().replaceAll("\\s+", " ");
        if (normalizedQuery.length() > 100) throw new InvalidNewsQueryException("News search must be 100 characters or fewer");
        String normalizedLanguage = language == null || language.isBlank() ? properties.getDefaultLanguage() : language.trim().toLowerCase(Locale.ROOT);
        if (!normalizedLanguage.matches("[a-z]{2,3}")) throw new InvalidNewsQueryException("Invalid news language");
        String regionValue = region == null || region.isBlank() ? properties.getDefaultRegion() : region;
        return new NewsQuery(
                NewsCategory.parse(category), NewsRegion.parse(regionValue), normalizedLanguage,
                normalizedQuery, normalizedPage, normalizedLimit);
    }
}
