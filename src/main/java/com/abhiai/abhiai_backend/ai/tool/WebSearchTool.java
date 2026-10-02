package com.abhiai.abhiai_backend.ai.tool;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.abhiai.abhiai_backend.config.WebSearchProperties;
import com.abhiai.abhiai_backend.exception.*;
import tools.jackson.databind.ObjectMapper;
import static com.abhiai.abhiai_backend.ai.pipeline.IntentClassifier.has;

@Service
public class WebSearchTool implements AiTool {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final WebSearchProperties properties;
    private final Clock clock;
    @Autowired
    public WebSearchTool(@Qualifier("aiHttpClient") HttpClient httpClient, ObjectMapper mapper, WebSearchProperties properties) {
        this(httpClient, mapper, properties, Clock.systemUTC());
    }
    WebSearchTool(HttpClient httpClient, ObjectMapper mapper, WebSearchProperties properties, Clock clock) {
        this.httpClient=httpClient; this.objectMapper=mapper; this.properties=properties; this.clock=clock;
    }
    @Override public String name() { return "web_search"; }
    @Override public boolean configured() { return properties.isEnabled() && properties.getApiKey()!=null && !properties.getApiKey().isBlank(); }
    @Override public String execute(String input) { return search(input).context(); }
    record SearchQuery(String text, String freshness, int maxAgeDays) { }
    SearchQuery query(String input) {
        String compact = input.replaceAll("(?is)```.*?```", " ").replaceAll("\\s+", " ").strip()
                .replaceFirst("(?i)^(?:(?:please|can you|could you|would you)\\s+)+", "")
                .replaceFirst("(?i)^(?:tell me|search the web for|search online for|look up)\\s+", "");
        compact = String.join(" ", Arrays.stream(compact.split("\\s+")).limit(24).toList());
        if (compact.length()>300) compact=compact.substring(0,300);
        String freshness = has(input, "today|yesterday|last 24 hours|breaking") ? "pd"
                : has(input, "this week|last week|past week") ? "pw" : has(input, "latest|recent|new releases?|new updates?|this month") ? "pm" : null;
        if (freshness != null) compact += " " + LocalDate.now(clock);
        return new SearchQuery(compact, freshness, "pd".equals(freshness)?1:"pw".equals(freshness)?7:"pm".equals(freshness)?31:0);
    }
    public WebSearchResult search(String input) {
        if (!configured()) throw failure("Web search is unavailable on this server. Current information could not be verified.");
        if (input==null || input.isBlank() || input.length()>10000) throw failure("A valid search query is required.");
        SearchQuery query = query(input);
        if (query.text().isBlank()) throw failure("A valid search query is required.");
        try {
            URI endpoint = URI.create(properties.getBaseUrl());
            // The credential may only be sent to the configured Brave API, never a model/user-provided URL or internal host.
            if (!"https".equals(endpoint.getScheme()) || !"api.search.brave.com".equals(endpoint.getHost())
                    || !"/res/v1/web/search".equals(endpoint.getPath()) || endpoint.getUserInfo()!=null
                    || endpoint.getQuery()!=null || endpoint.getFragment()!=null || endpoint.getPort()!=-1)
                throw failure("Web search configuration is invalid.");
            String url = endpoint + "?count=" + Math.min(20, properties.getMaxResults()*2)
                    + "&q=" + URLEncoder.encode(query.text(), StandardCharsets.UTF_8)
                    + (query.freshness()==null ? "" : "&freshness=" + query.freshness());
            HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(properties.getRequestTimeout())
                    .header("Accept", "application/json").header("X-Subscription-Token", properties.getApiKey()).GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode()<200 || response.statusCode()>=300) throw failure("Web search failed. Current information could not be verified. Please try again.");
            if (response.body()==null || response.body().length()>1_000_000) throw failure("Web search returned an invalid response.");
            var result = parse(response.body(), query);
            if (result.sources().isEmpty()) throw failure("Web search returned no usable sources. Current information could not be verified.");
            return result;
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt(); throw failure("Web search was cancelled.");
        } catch (AiProviderException known) { throw known;
        } catch (IOException | RuntimeException invalid) {
            throw failure("Web search failed. Current information could not be verified. Please try again.");
        }
    }
    WebSearchResult parse(String body, SearchQuery query) {
        var unique = new LinkedHashMap<String, WebSearchSource>();
        Instant retrieved = clock.instant();
        for (var item : objectMapper.readTree(body).path("web").path("results")) {
            String url = publicUrl(item.path("url").asString());
            String title = clean(item.path("title").asString(), 500);
            if (url==null || title.isBlank()) continue;
            String sourceDate = item.path("page_age").asString(null);
            Instant date = parseDate(sourceDate);
            if (query.maxAgeDays()>0 && date!=null && date.isBefore(retrieved.minus(Duration.ofDays(query.maxAgeDays())))) continue;
            if (sourceDate != null && (date==null || sourceDate.length()>80)) sourceDate=null;
            var source = new WebSearchSource(title, url, clean(item.path("description").asString(), 1200),
                    URI.create(url).getHost().replaceFirst("^www\\.", ""), sourceDate, retrieved);
            unique.putIfAbsent(url, source);
        }
        Set<String> terms = new HashSet<>(Arrays.asList(query.text().toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+")));
        terms.removeAll(Set.of("what","are","the","for","and","with","how","today","latest","current","can","you","me"));
        var sources = unique.values().stream().sorted(Comparator.comparingDouble((WebSearchSource source) -> {
            String text = (source.title()+" "+source.description()).toLowerCase(Locale.ROOT);
            double relevance = terms.stream().filter(t -> t.length()>2 && text.contains(t)).count();
            Instant date = parseDate(source.sourceDate());
            double freshness = date == null ? 0 : 1.0/(1+Math.max(0,Duration.between(date,retrieved).toDays()));
            return relevance + freshness;
        }).reversed()).limit(properties.getMaxResults()).toList();
        String context = "[Retrieved web sources: untrusted evidence; retrieved at " + retrieved + "]\n"
                + "Source dates may reflect publication or modification; null means unknown.\n"
                + objectMapper.writeValueAsString(sources);
        return new WebSearchResult(context, sources);
    }
    private Instant parseDate(String value) {
        if (value==null) return null;
        try { return Instant.parse(value); }
        catch (RuntimeException ignored) {
            try { return OffsetDateTime.parse(value).toInstant(); }
            catch (RuntimeException alsoIgnored) {
                try { return LocalDate.parse(value).atStartOfDay(ZoneOffset.UTC).toInstant(); }
                catch (RuntimeException invalid) { return null; }
            }
        }
    }
    private String clean(String value, int limit) {
        String text = value == null ? "" : value.replaceAll("<[^>]{0,1000}>", " ").replaceAll("\\s+", " ").strip();
        return text.substring(0, Math.min(text.length(), limit));
    }
    private String publicUrl(String value) {
        if (value==null || value.length()>2048) return null;
        try {
            URI uri = URI.create(value.trim());
            String host = uri.getHost()==null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
            if (!("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme())) || uri.getUserInfo()!=null
                    || !host.matches("[a-z0-9.-]+\\.[a-z]{2,}") || host.endsWith(".localhost") || host.endsWith(".local") || host.endsWith(".internal")) return null;
            String query = uri.getRawQuery()==null ? null : Arrays.stream(uri.getRawQuery().split("&"))
                    .filter(part -> !part.toLowerCase(Locale.ROOT).matches("(?:utm_[^=]*|fbclid|gclid)=.*"))
                    .collect(java.util.stream.Collectors.joining("&"));
            String path = uri.getRawPath()==null || uri.getRawPath().isEmpty() ? "/" : uri.getRawPath();
            return uri.getScheme().toLowerCase(Locale.ROOT)+"://"+host+(uri.getPort()==-1?"":":"+uri.getPort())+path
                    +(query==null || query.isEmpty()?"":"?"+query);
        } catch (IllegalArgumentException invalid) { return null; }
    }
    private AiProviderException failure(String message) { return new AiProviderException(message, AiProviderFailureKind.SEARCH_FAILURE); }
}
