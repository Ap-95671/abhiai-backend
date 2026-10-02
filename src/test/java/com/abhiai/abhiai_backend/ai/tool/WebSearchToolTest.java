package com.abhiai.abhiai_backend.ai.tool;

import java.net.http.*;
import java.time.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import com.abhiai.abhiai_backend.config.WebSearchProperties;
import com.abhiai.abhiai_backend.exception.*;
import tools.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WebSearchToolTest {
    private final HttpClient client=mock(HttpClient.class);
    private final WebSearchProperties properties=new WebSearchProperties();
    private WebSearchTool tool(){
        properties.setEnabled(true);properties.setApiKey("test-search-key");
        return new WebSearchTool(client,new ObjectMapper(),properties,Clock.fixed(Instant.parse("2026-10-02T12:00:00Z"),ZoneOffset.UTC));
    }
    @SuppressWarnings("unchecked") private void response(int status,String body)throws Exception{
        HttpResponse<String> response=mock(HttpResponse.class);when(response.statusCode()).thenReturn(status);when(response.body()).thenReturn(body);
        when(client.send(any(),any(HttpResponse.BodyHandler.class))).thenReturn(response);
    }
    @Test void successfulSearchUsesFreshnessAndPreservesBoundedDeduplicatedSources()throws Exception{
        response(200,"""
            {"web":{"results":[
            {"title":"Old OpenAI news","url":"https://example.com/old","description":"Old","page_age":"2020-01-01"},
            {"title":"Latest OpenAI release","url":"https://example.com/release?utm_source=test#fragment","description":"A new OpenAI release","page_age":"2026-10-02T10:00:00Z"},
            {"title":"Duplicate","url":"https://example.com/release","description":"duplicate"},
            {"title":"Source without date","url":"https://other.example.org/news","description":"More OpenAI developments"},
            {"title":"Unsafe","url":"http://127.0.0.1/admin","description":"private"},
            {"title":"Unsafe scheme","url":"javascript:alert(1)"}
            ]}}
            """);
        var result=tool().search("Can you tell me the latest OpenAI developments today?");
        assertThat(result.sources()).hasSize(2);
        assertThat(result.sources().getFirst().url()).isEqualTo("https://example.com/release");
        assertThat(result.sources().getFirst().sourceDate()).isEqualTo("2026-10-02T10:00:00Z");
        assertThat(result.sources().getFirst().retrievedAt()).isEqualTo(Instant.parse("2026-10-02T12:00:00Z"));
        assertThat(result.sources().get(1).sourceDate()).isNull();
        assertThat(result.context()).contains("untrusted evidence","A new OpenAI release").doesNotContain("Old OpenAI news");
        var request=ArgumentCaptor.forClass(HttpRequest.class);verify(client).send(request.capture(),any(HttpResponse.BodyHandler.class));
        assertThat(request.getValue().uri().toString()).contains("freshness=pd","2026-10-02").doesNotContain("Can+you");
        assertThat(request.getValue().headers().firstValue("X-Subscription-Token")).contains("test-search-key");
    }
    @Test void currentCeoDoesNotRequireRecentPublicationDate()throws Exception{
        response(200,"{\"web\":{\"results\":[{\"title\":\"Leadership\",\"url\":\"https://example.com/leadership\",\"description\":\"Current CEO\",\"page_age\":\"2020-01-01\"}]}}");
        var tool=tool();assertThat(tool.query("current OpenAI CEO").freshness()).isNull();
        assertThat(tool.search("current OpenAI CEO").sources()).hasSize(1);
    }
    @Test void queryAndSnippetSizesAreBoundedWithoutForwardingEntireLongPrompt(){
        var tool=tool();var query=tool.query("Please search the web for latest releases " + "word ".repeat(1000));
        assertThat(query.text().length()).isLessThanOrEqualTo(311);
        var result=tool.parse("{\"web\":{\"results\":[{\"title\":\"Title\",\"url\":\"https://example.com/a\",\"description\":\""+"a".repeat(4000)+"\"}]}}",query);
        assertThat(result.sources().getFirst().description()).hasSize(1200);
    }
    @Test void failuresAndMalformedOrEmptyResultsAreNormalized()throws Exception{
        for(String body:java.util.List.of("not-json","{}","{\"web\":{\"results\":[]}}")){
            response(200,body);
            assertThatThrownBy(()->tool().search("latest news")).isInstanceOfSatisfying(AiProviderException.class,e->assertThat(e.kind()).isEqualTo(AiProviderFailureKind.SEARCH_FAILURE));
        }
        response(429,"secret upstream diagnostic");
        assertThatThrownBy(()->tool().search("latest news")).hasMessageNotContaining("secret");
    }
    @Test void configurationAndToolEndpointCannotTargetInternalNetwork(){
        var tool=tool();properties.setBaseUrl("http://127.0.0.1/secrets");
        assertThatThrownBy(()->tool.search("hello")).isInstanceOf(AiProviderException.class);
        verifyNoInteractions(client);
        properties.setEnabled(false);
        assertThatThrownBy(()->tool.search("latest news")).hasMessageContaining("could not be verified");
    }
}
