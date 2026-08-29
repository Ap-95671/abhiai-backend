package com.abhiai.abhiai_backend.news.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.abhiai.abhiai_backend.news.cache.NewsCache;
import com.abhiai.abhiai_backend.news.config.NewsProperties;
import com.abhiai.abhiai_backend.news.exception.InvalidNewsQueryException;
import com.abhiai.abhiai_backend.news.model.NewsArticle;
import com.abhiai.abhiai_backend.news.model.NewsCategory;
import com.abhiai.abhiai_backend.news.model.NewsQuery;
import com.abhiai.abhiai_backend.news.model.NewsRegion;
import com.abhiai.abhiai_backend.news.model.NewsSource;
import com.abhiai.abhiai_backend.news.provider.NewsProvider;
import com.abhiai.abhiai_backend.news.provider.NewsProviderPage;

class NewsServiceTest {

    @Test
    void normalizesSearchCategoryRegionAndReturnsTheRequestedPage() {
        AtomicReference<NewsQuery> captured = new AtomicReference<>();
        List<NewsArticle> articles = new ArrayList<>();
        for (int index = 0; index < 12; index++) articles.add(article(index));
        NewsService service = service(provider(captured, articles));

        var response = service.list("ai-tech", "asia", "EN", "  OpenAI   models  ", 1, 5, false);

        assertThat(captured.get().category()).isEqualTo(NewsCategory.AI_TECH);
        assertThat(captured.get().region()).isEqualTo(NewsRegion.ASIA);
        assertThat(captured.get().language()).isEqualTo("en");
        assertThat(captured.get().query()).isEqualTo("OpenAI models");
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.limit()).isEqualTo(5);
        assertThat(response.content()).hasSize(5);
    }

    @Test
    void returnsAStableEmptyPageWhenTheProviderHasNoStories() {
        NewsService service = service(provider(new AtomicReference<>(), List.of()));

        var response = service.list("top", "global", null, null, null, null, false);

        assertThat(response.content()).isEmpty();
        assertThat(response.hasMore()).isFalse();
        assertThat(response.stale()).isFalse();
    }

    @Test
    void rejectsInvalidFiltersBeforeCallingTheProvider() {
        NewsService service = service(provider(new AtomicReference<>(), List.of()));

        assertThatThrownBy(() -> service.list("unknown", "global", "en", null, 0, 10, false))
                .isInstanceOf(InvalidNewsQueryException.class);
        assertThatThrownBy(() -> service.list("latest", "global", "en", null, -1, 10, false))
                .isInstanceOf(InvalidNewsQueryException.class);
        assertThatThrownBy(() -> service.list("latest", "global", "en", null, 0, 21, false))
                .isInstanceOf(InvalidNewsQueryException.class);
    }

    private NewsService service(NewsProvider provider) {
        NewsProperties properties = new NewsProperties();
        NewsCache cache = new NewsCache(
                provider,
                new NewsArticleRanker(),
                properties,
                Clock.fixed(Instant.parse("2026-08-29T12:00:00Z"), ZoneOffset.UTC));
        return new NewsService(cache, properties);
    }

    private NewsProvider provider(AtomicReference<NewsQuery> captured, List<NewsArticle> articles) {
        return new NewsProvider() {
            @Override public String providerName() { return "test"; }
            @Override public boolean configured() { return true; }
            @Override public NewsProviderPage fetch(NewsQuery query, String cursor) {
                captured.set(query);
                return new NewsProviderPage(articles, null, articles.size());
            }
            @Override public Optional<NewsArticle> fetchById(String articleId) { return Optional.empty(); }
        };
    }

    private NewsArticle article(int index) {
        String[] topics = {
                "Earthquake response expands across coastal towns",
                "Space telescope captures a distant stellar nursery",
                "Central bank announces a revised interest rate",
                "National football team reaches tournament final",
                "Researchers publish a new battery chemistry study",
                "Parliament approves an updated privacy framework",
                "Film festival reveals this year's award winners",
                "Technology companies agree on an AI safety standard",
                "Wildlife survey records growth in forest habitats",
                "Shipping operators open a new international route",
                "Health agency releases updated vaccination guidance",
                "Weather service tracks a major Atlantic storm"
        };
        return new NewsArticle(
                "story-" + index,
                topics[index],
                "Summary " + index,
                "Source " + index,
                "https://source.example",
                "https://example.com/story-" + index,
                null,
                Instant.parse("2026-08-29T10:00:00Z").minusSeconds(index * 60L),
                "world",
                "",
                "global",
                "en",
                "",
                "test",
                1,
                List.of(new NewsSource("Source " + index, "https://source.example")));
    }
}
