package com.abhiai.abhiai_backend.news.provider;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.abhiai.abhiai_backend.news.config.NewsProperties;
import com.abhiai.abhiai_backend.news.exception.NewsProviderException;
import com.abhiai.abhiai_backend.news.model.NewsArticle;
import com.abhiai.abhiai_backend.news.model.NewsQuery;
import com.abhiai.abhiai_backend.news.model.NewsSource;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
@ConditionalOnProperty(name = "app.news.provider", havingValue = "newsdata", matchIfMissing = true)
public class NewsDataProvider implements NewsProvider {

    private static final DateTimeFormatter NEWSDATA_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final NewsProperties properties;

    public NewsDataProvider(
            @Qualifier("newsHttpClient") HttpClient httpClient,
            ObjectMapper objectMapper,
            NewsProperties properties) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override public String providerName() { return "newsdata"; }

    @Override
    public boolean configured() {
        return properties.getApiKey() != null && !properties.getApiKey().isBlank();
    }

    @Override
    public NewsProviderPage fetch(NewsQuery query, String cursor) {
        ensureConfigured();
        return request(buildUrl(query, cursor, null), query.region().id());
    }

    @Override
    public Optional<NewsArticle> fetchById(String articleId) {
        ensureConfigured();
        NewsQuery query = new NewsQuery(
                com.abhiai.abhiai_backend.news.model.NewsCategory.LATEST,
                com.abhiai.abhiai_backend.news.model.NewsRegion.GLOBAL,
                properties.getDefaultLanguage(), "", 0, 1);
        return request(buildUrl(query, null, articleId), "global").articles().stream().findFirst();
    }

    NewsProviderPage request(URI uri, String region) {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(properties.getRequestTimeout())
                .header("Accept", "application/json")
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new NewsProviderException("News is temporarily unavailable");
            }
            return mapResponse(objectMapper.readTree(response.body()), region);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new NewsProviderException("News request was interrupted", exception);
        } catch (IOException | RuntimeException exception) {
            if (exception instanceof NewsProviderException providerException) throw providerException;
            throw new NewsProviderException("News is temporarily unavailable", exception);
        }
    }

    NewsProviderPage mapResponse(JsonNode payload, String region) {
        if (!"success".equalsIgnoreCase(payload.path("status").asString())) {
            throw new NewsProviderException("News is temporarily unavailable");
        }
        List<NewsArticle> articles = new ArrayList<>();
        for (JsonNode item : payload.path("results")) {
            String id = text(item, "article_id");
            String title = compact(text(item, "title"));
            String articleUrl = safeUrl(text(item, "link"));
            if (id.isBlank() || title.isBlank() || articleUrl == null) continue;
            String sourceName = firstNonBlank(text(item, "source_name"), text(item, "source_id"), "Unknown source");
            String sourceUrl = safeUrl(text(item, "source_url"));
            String description = truncate(compact(text(item, "description")), 520);
            String category = firstArrayValue(item.path("category"), "world");
            String country = String.join(", ", arrayValues(item.path("country")));
            String author = firstArrayValue(item.path("creator"), "");
            articles.add(new NewsArticle(
                    id,
                    title,
                    description,
                    sourceName,
                    sourceUrl,
                    articleUrl,
                    safeUrl(text(item, "image_url")),
                    parseDate(text(item, "pubDate")),
                    category,
                    country,
                    region,
                    firstNonBlank(text(item, "language"), "en"),
                    author,
                    providerName(),
                    1,
                    List.of(new NewsSource(sourceName, sourceUrl))));
        }
        String nextCursor = compact(payload.path("nextPage").asString());
        return new NewsProviderPage(articles, nextCursor.isBlank() ? null : nextCursor, payload.path("totalResults").asLong(articles.size()));
    }

    private URI buildUrl(NewsQuery query, String cursor, String articleId) {
        List<String> params = new ArrayList<>();
        add(params, "apikey", properties.getApiKey());
        add(params, "language", query.language());
        add(params, "size", String.valueOf(Math.min(10, Math.max(1, query.limit()))));
        add(params, "removeduplicate", "1");
        if (articleId != null) add(params, "id", articleId);
        else {
            add(params, "q", query.query());
            add(params, "category", query.category().providerValue());
            add(params, "country", query.region().countryCodes());
            add(params, "page", cursor);
        }
        String separator = properties.getBaseUrl().contains("?") ? "&" : "?";
        return URI.create(properties.getBaseUrl() + separator + String.join("&", params));
    }

    private void ensureConfigured() {
        if (!configured()) throw new NewsProviderException("News is not configured yet");
    }

    private static void add(List<String> params, String key, String value) {
        if (value == null || value.isBlank()) return;
        params.add(encode(key) + "=" + encode(value));
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String text(JsonNode node, String field) {
        return compact(node.path(field).asString());
    }

    private static String compact(String value) {
        return value == null ? "" : value.replaceAll("\\s+", " ").trim();
    }

    private static String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max - 1).trim() + "…";
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) if (value != null && !value.isBlank()) return value;
        return "";
    }

    private static List<String> arrayValues(JsonNode node) {
        List<String> values = new ArrayList<>();
        if (node.isArray()) for (JsonNode item : node) if (!item.asString().isBlank()) values.add(item.asString());
        else if (!node.asString().isBlank()) values.add(node.asString());
        return values;
    }

    private static String firstArrayValue(JsonNode node, String fallback) {
        List<String> values = arrayValues(node);
        return values.isEmpty() ? fallback : values.get(0);
    }

    private static String safeUrl(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            URI uri = URI.create(value.trim());
            return ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null ? uri.toString() : null;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static Instant parseDate(String value) {
        if (value == null || value.isBlank()) return Instant.EPOCH;
        try { return Instant.parse(value); }
        catch (DateTimeParseException ignored) {
            try { return LocalDateTime.parse(value, NEWSDATA_DATE).toInstant(ZoneOffset.UTC); }
            catch (DateTimeParseException invalid) { return Instant.EPOCH; }
        }
    }
}
