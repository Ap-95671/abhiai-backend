package com.abhiai.abhiai_backend.news.cache;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.abhiai.abhiai_backend.news.config.NewsProperties;
import com.abhiai.abhiai_backend.news.exception.NewsProviderException;
import com.abhiai.abhiai_backend.news.model.NewsArticle;
import com.abhiai.abhiai_backend.news.model.NewsQuery;
import com.abhiai.abhiai_backend.news.provider.NewsProvider;
import com.abhiai.abhiai_backend.news.provider.NewsProviderPage;
import com.abhiai.abhiai_backend.news.service.NewsArticleRanker;

@Component
public class NewsCache {

    private static final int MAX_PROVIDER_PAGES_PER_REQUEST = 4;
    private final NewsProvider provider;
    private final NewsArticleRanker ranker;
    private final NewsProperties properties;
    private final Clock clock;
    private final Map<String, CachedFeed> feeds = new ConcurrentHashMap<>();

    public NewsCache(NewsProvider provider, NewsArticleRanker ranker, NewsProperties properties, Clock newsClock) {
        this.provider = provider;
        this.ranker = ranker;
        this.properties = properties;
        this.clock = newsClock;
    }

    public CachedNewsPage get(NewsQuery query, boolean refresh) {
        String key = query.cacheKey();
        CachedFeed feed = feeds.computeIfAbsent(key, ignored -> new CachedFeed());
        synchronized (feed) {
            Instant now = clock.instant();
            boolean expired = !feed.initialized || feed.updatedAt.plus(properties.getRefreshInterval()).isBefore(now);
            if (refresh || expired) refresh(feed, query.withoutPage(), now);

            int from = query.page() * query.limit();
            int targetSize = from + query.limit();
            int providerCalls = 0;
            while (feed.articles.size() < targetSize && feed.nextCursor != null && providerCalls++ < MAX_PROVIDER_PAGES_PER_REQUEST) {
                append(feed, query.withoutPage(), feed.nextCursor, now);
            }
            int to = Math.min(targetSize, feed.articles.size());
            List<NewsArticle> page = from >= feed.articles.size() ? List.of() : List.copyOf(feed.articles.subList(from, to));
            boolean hasMore = to < feed.articles.size() || feed.nextCursor != null;
            return new CachedNewsPage(page, feed.totalResults, hasMore, feed.updatedAt, feed.stale);
        }
    }

    public Optional<NewsArticle> find(String articleId) {
        for (CachedFeed feed : feeds.values()) {
            synchronized (feed) {
                Optional<NewsArticle> cached = feed.articles.stream().filter(article -> article.id().equals(articleId)).findFirst();
                if (cached.isPresent()) return cached;
            }
        }
        return provider.fetchById(articleId);
    }

    private void refresh(CachedFeed feed, NewsQuery query, Instant now) {
        List<NewsArticle> staleArticles = feed.articles;
        Instant staleUpdatedAt = feed.updatedAt;
        try {
            NewsProviderPage first = provider.fetch(query, null);
            feed.articles = ranker.mergeAndRank(List.of(), first.articles());
            feed.nextCursor = first.nextCursor();
            feed.totalResults = first.totalResults();
            feed.updatedAt = now;
            feed.initialized = true;
            feed.stale = false;
        } catch (NewsProviderException exception) {
            if (!staleArticles.isEmpty() && staleUpdatedAt.plus(properties.getStaleRetention()).isAfter(now)) {
                feed.articles = staleArticles;
                feed.updatedAt = staleUpdatedAt;
                feed.initialized = true;
                feed.stale = true;
                return;
            }
            throw exception;
        }
    }

    private void append(CachedFeed feed, NewsQuery query, String cursor, Instant now) {
        try {
            NewsProviderPage next = provider.fetch(query, cursor);
            feed.articles = ranker.mergeAndRank(feed.articles, next.articles());
            feed.nextCursor = next.nextCursor();
            feed.totalResults = Math.max(feed.totalResults, next.totalResults());
            feed.updatedAt = now;
            feed.stale = false;
        } catch (NewsProviderException exception) {
            feed.stale = true;
            feed.nextCursor = null;
        }
    }

    private static final class CachedFeed {
        private List<NewsArticle> articles = new ArrayList<>();
        private String nextCursor;
        private long totalResults;
        private Instant updatedAt = Instant.EPOCH;
        private boolean initialized;
        private boolean stale;
    }

    public record CachedNewsPage(
            List<NewsArticle> articles,
            long totalResults,
            boolean hasMore,
            Instant updatedAt,
            boolean stale) {}
}
