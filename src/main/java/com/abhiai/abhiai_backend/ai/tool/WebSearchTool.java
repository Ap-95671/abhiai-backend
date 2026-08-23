package com.abhiai.abhiai_backend.ai.tool;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

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
            StringBuilder context = new StringBuilder("[Consented web search results]\n");
            int index = 1;
            for (var result : objectMapper.readTree(response.body()).path("web").path("results")) {
                context.append(index++).append(". ")
                        .append(result.path("title").asString()).append("\n")
                        .append(result.path("url").asString()).append("\n")
                        .append(result.path("description").asString()).append("\n\n");
            }
            if (index == 1) {
                return "[Consented web search returned no results]";
            }
            return context.toString();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AiProviderException("Web search was interrupted", exception);
        } catch (IOException exception) {
            throw new AiProviderException("Web search failed", exception);
        }
    }
}
