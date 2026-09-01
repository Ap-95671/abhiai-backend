package com.abhiai.abhiai_backend.ai.tool;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.abhiai.abhiai_backend.config.WebSearchProperties;
import com.abhiai.abhiai_backend.exception.AiProviderException;

import tools.jackson.databind.ObjectMapper;

@Service
public class WebSearchTool implements AiTool {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final WebSearchProperties properties;

    public WebSearchTool(
            @Qualifier("aiHttpClient") HttpClient httpClient,
            ObjectMapper objectMapper,
            WebSearchProperties properties) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public String name() {
        return "web_search";
    }

    @Override
    public boolean configured() {
        return properties.isEnabled()
                && properties.getApiKey() != null
                && !properties.getApiKey().isBlank();
    }

    @Override
    public String execute(String input) {
        return search(input).context();
    }

    public WebSearchResult search(String input) {
        if (!configured()) {
            throw new AiProviderException("Web search is not configured on this server");
        }
        String url = properties.getBaseUrl() + "?count=5&q="
                + URLEncoder.encode(input, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(properties.getRequestTimeout())
                .header("Accept", "application/json")
                .header("X-Subscription-Token", properties.getApiKey())
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new AiProviderException("Web search could not complete the request");
            }
            List<WebSearchSource> sources = new ArrayList<>();
            for (var result : objectMapper.readTree(response.body()).path("web").path("results")) {
                String resultUrl = validHttpUrl(result.path("url").asString());
                String title = result.path("title").asString().trim();
                if (resultUrl == null || title.isBlank()) continue;
                URI uri = URI.create(resultUrl);
                sources.add(new WebSearchSource(
                        title,
                        resultUrl,
                        result.path("description").asString().trim(),
                        uri.getHost() == null ? "Source" : uri.getHost().replaceFirst("^www\\.", "")));
            }
            if (sources.isEmpty()) {
                return new WebSearchResult("[Consented web search returned no results]", List.of());
            }
            StringBuilder context = new StringBuilder("[Consented web search results]\n");
            for (int sourceIndex = 0; sourceIndex < sources.size(); sourceIndex++) {
                WebSearchSource source = sources.get(sourceIndex);
                context.append(sourceIndex + 1).append(". ")
                        .append(source.title()).append("\n")
                        .append(source.url()).append("\n")
                        .append(source.description()).append("\n\n");
            }
            return new WebSearchResult(context.toString(), sources);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AiProviderException("Web search was interrupted", exception);
        } catch (IOException exception) {
            throw new AiProviderException("Web search failed", exception);
        }
    }

    private String validHttpUrl(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            URI uri = URI.create(value.trim());
            return ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null ? uri.toString() : null;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
