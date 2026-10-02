package com.abhiai.abhiai_backend.ai.tool;

import java.util.List;
import org.junit.jupiter.api.Test;
import com.abhiai.abhiai_backend.exception.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.anyString;

class AiToolRegistryTest {
    @Test void autoSearchIsCompositionalAndTimelessQuestionsDoNotSearch(){
        var search=mock(WebSearchTool.class);var tools=new AiToolRegistry(search);
        when(search.search(anyString())).thenReturn(new WebSearchResult("evidence",List.of(new WebSearchSource("Title","https://example.com","Snippet","example.com"))));
        for(String prompt:List.of("What is binary search?","Explain merge sort.","Explain electrical current","Today I want to learn binary search"))
            assertThat(tools.augmentPromptWithSources(prompt,false).sources()).isEmpty();
        verifyNoInteractions(search);
        for(String prompt:List.of("latest AI news today","current OpenAI CEO","What happened with OpenAI today?","Search the web for the latest OpenAI updates.","Who is the CEO of OpenAI?"))
            assertThat(tools.augmentPromptWithSources(prompt,false).prompt()).contains("evidence");
        verify(search,times(5)).search(anyString());
    }
    @Test void manualSearchForcesToolAndImagesBypassIt(){
        var search=mock(WebSearchTool.class);var tools=new AiToolRegistry(search);
        when(search.search(anyString())).thenReturn(new WebSearchResult("source",List.of()));
        tools.augmentPromptWithSources("Explain merge sort",true);
        verify(search).search("Explain merge sort");
        tools.augmentPromptWithSources("Generate an image of a robot",true);
        verifyNoMoreInteractions(search);
    }
    @Test void invalidToolOrPermissionIsBlockedAndSearchFailureIsNotSwallowed(){
        var search=mock(WebSearchTool.class);var tools=new AiToolRegistry(search);
        assertThatThrownBy(()->tools.executeTool("read_environment","keys",true)).isInstanceOf(AiProviderException.class);
        assertThatThrownBy(()->tools.executeTool("web_search","query",false)).isInstanceOf(AiProviderException.class);
        verifyNoInteractions(search);
        when(search.search(anyString())).thenThrow(new AiProviderException("Search failed",AiProviderFailureKind.SEARCH_FAILURE));
        assertThatThrownBy(()->tools.augmentPromptWithSources("latest news",false)).hasMessage("Search failed");
    }
}
