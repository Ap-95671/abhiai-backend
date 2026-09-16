package com.abhiai.abhiai_backend.ai.gemini;
import java.net.http.HttpClient;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.abhiai.abhiai_backend.ai.*;
import com.abhiai.abhiai_backend.entity.MessageRole;
import tools.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class GeminiV2RequestTest {
    @Test void systemInstructionsStaySeparateFromUntrustedPageAndUserContent() {
        var mapper=new ObjectMapper();var provider=new GeminiProvider(mock(HttpClient.class),mapper,new GeminiProperties());
        var payload=mapper.readTree(provider.buildRequestBody(new AiChatRequest(List.of(
            new AiChatMessage(MessageRole.SYSTEM,"Safety rules"),new AiChatMessage(MessageRole.USER,"Untrusted page excerpt"),
            new AiChatMessage(MessageRole.USER,"Summarize")))));
        assertTrue(payload.path("systemInstruction").toString().contains("Safety rules"));
        assertFalse(payload.path("contents").toString().contains("Safety rules"));
        assertEquals(2,payload.path("contents").size());
    }
}
