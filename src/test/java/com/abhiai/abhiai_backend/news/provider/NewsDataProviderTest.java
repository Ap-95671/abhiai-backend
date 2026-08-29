package com.abhiai.abhiai_backend.news.provider;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpClient;

import org.junit.jupiter.api.Test;

import com.abhiai.abhiai_backend.news.config.NewsProperties;

import tools.jackson.databind.ObjectMapper;

class NewsDataProviderTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void normalizesMissingOptionalFieldsWithoutBrokenUrls() {
        NewsDataProvider provider = new NewsDataProvider(HttpClient.newHttpClient(), mapper, new NewsProperties());
        var payload = mapper.readTree("""
                {"status":"success","totalResults":1,"results":[{
                  "article_id":"story-1","title":"A useful headline","link":"https://publisher.example/story",
                  "source_id":"publisher","pubDate":"2026-08-29 10:15:00","image_url":"javascript:alert(1)",
                  "category":["technology"],"country":["india"],"language":"english"
                }]}
                """);

        NewsProviderPage result = provider.mapResponse(payload, "global");

        assertThat(result.articles()).hasSize(1);
        assertThat(result.articles().getFirst().imageUrl()).isNull();
        assertThat(result.articles().getFirst().description()).isEmpty();
        assertThat(result.articles().getFirst().sourceName()).isEqualTo("publisher");
    }

    @Test
    void skipsArticlesWithoutSafePublisherLinks() {
        NewsDataProvider provider = new NewsDataProvider(HttpClient.newHttpClient(), mapper, new NewsProperties());
        var payload = mapper.readTree("""
                {"status":"success","results":[{"article_id":"story-1","title":"Headline","link":"data:text/plain,bad"}]}
                """);

        assertThat(provider.mapResponse(payload, "global").articles()).isEmpty();
    }
}
