package com.abhiai.abhiai_backend.news.cache;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.abhiai.abhiai_backend.news.config.NewsProperties;
import com.abhiai.abhiai_backend.news.exception.NewsProviderException;
import com.abhiai.abhiai_backend.news.model.NewsArticle;
import com.abhiai.abhiai_backend.news.model.NewsCategory;
import com.abhiai.abhiai_backend.news.model.NewsQuery;
import com.abhiai.abhiai_backend.news.model.NewsRegion;
import com.abhiai.abhiai_backend.news.model.NewsSource;
import com.abhiai.abhiai_backend.news.provider.NewsProvider;
import com.abhiai.abhiai_backend.news.provider.NewsProviderPage;
import com.abhiai.abhiai_backend.news.service.NewsArticleRanker;

class NewsCacheTest {

    @Test
    void reusesFreshResultsAcrossRequests() {
        AtomicInteger calls = new AtomicInteger();
        NewsCache cache = cache(provider(calls, false), new MutableClock(Instant.parse("2026-08-29T10:00:00Z")));
        NewsQuery query = query();

        cache.get(query, false);
        cache.get(query, false);

        assertThat(calls).hasValue(1);
    }

    @Test
    void servesStaleResultsWhenRefreshFails() {
        AtomicInteger calls = new AtomicInteger();
        MutableClock clock = new MutableClock(Instant.parse("2026-08-29T10:00:00Z"));
        NewsProvider provider = provider(calls, true);
        NewsCache cache = cache(provider, clock);

        NewsCache.CachedNewsPage first = cache.get(query(), false);
        clock.advance(Duration.ofMinutes(20));
        NewsCache.CachedNewsPage stale = cache.get(query(), false);

        assertThat(first.stale()).isFalse();
        assertThat(stale.stale()).isTrue();
        assertThat(stale.articles()).hasSize(1);
    }

    private NewsCache cache(NewsProvider provider, Clock clock) {
        NewsProperties properties = new NewsProperties();
        properties.setRefreshInterval(Duration.ofMinutes(15));
        properties.setStaleRetention(Duration.ofHours(6));
        return new NewsCache(provider, new NewsArticleRanker(), properties, clock);
    }

    private NewsProvider provider(AtomicInteger calls, boolean failAfterFirst) {
        return new NewsProvider() {
            @Override public String providerName() { return "test"; }
            @Override public boolean configured() { return true; }
            @Override public NewsProviderPage fetch(NewsQuery query, String cursor) {
                int call = calls.incrementAndGet();
                if (failAfterFirst && call > 1) throw new NewsProviderException("unavailable");
                return new NewsProviderPage(List.of(article()), null, 1);
            }
            @Override public Optional<NewsArticle> fetchById(String articleId) { return Optional.empty(); }
        };
    }

    private NewsQuery query() {
        return new NewsQuery(NewsCategory.LATEST, NewsRegion.GLOBAL, "en", "", 0, 10);
    }

    private NewsArticle article() {
        return new NewsArticle("story", "Global story", "Summary", "Source", null, "https://example.com/story", null,
                Instant.parse("2026-08-29T09:00:00Z"), "world", "", "global", "en", "", "test", 1,
                List.of(new NewsSource("Source", null)));
    }

    private static final class MutableClock extends Clock {
        private Instant instant;
        private MutableClock(Instant instant) { this.instant = instant; }
        void advance(Duration duration) { instant = instant.plus(duration); }
        @Override public ZoneId getZone() { return ZoneId.of("UTC"); }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return instant; }
    }
}
