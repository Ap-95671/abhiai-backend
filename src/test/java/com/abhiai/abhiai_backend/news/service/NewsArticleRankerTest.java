package com.abhiai.abhiai_backend.news.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.abhiai.abhiai_backend.news.model.NewsArticle;
import com.abhiai.abhiai_backend.news.model.NewsSource;

class NewsArticleRankerTest {

    private final NewsArticleRanker ranker = new NewsArticleRanker();

    @Test
    void clustersHighlySimilarHeadlinesAndKeepsTheirSources() {
        NewsArticle first = article("one", "Major earthquake strikes coastal city", "Reuters");
        NewsArticle second = article("two", "Major earthquake strikes coastal city today", "BBC");

        List<NewsArticle> ranked = ranker.mergeAndRank(List.of(first), List.of(second));

        assertThat(ranked).hasSize(1);
        assertThat(ranked.getFirst().relatedStoryCount()).isEqualTo(2);
        assertThat(ranked.getFirst().sources()).extracting(NewsSource::name).containsExactly("Reuters", "BBC");
    }

    @Test
    void keepsDifferentStoriesSeparate() {
        List<NewsArticle> ranked = ranker.mergeAndRank(
                List.of(article("one", "Central bank changes interest rates", "Reuters")),
                List.of(article("two", "Space agency launches lunar probe", "AP")));

        assertThat(ranked).hasSize(2);
    }

    private NewsArticle article(String id, String title, String source) {
        return new NewsArticle(id, title, "Summary", source, "https://source.example", "https://example.com/" + id,
                null, Instant.parse("2026-08-29T10:00:00Z"), "world", "", "global", "en", "", "newsdata", 1,
                List.of(new NewsSource(source, "https://source.example")));
    }
}
