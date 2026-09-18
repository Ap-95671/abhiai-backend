package com.abhiai.abhiai_backend.assistant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.abhiai.abhiai_backend.config.SecurityConfig;
import com.abhiai.abhiai_backend.security.JwtAuthenticationFilter;
import com.abhiai.abhiai_backend.security.JwtService;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.*;

@WebMvcTest({AssistantController.class, AssistantToolsController.class, AgentController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class AssistantSecurityTest {
    @Autowired MockMvc mvc;
    @MockitoBean JwtService jwtService;
    @MockitoBean AssistantProperties settings;
    @MockitoBean AssistantPolicy policy;
    @MockitoBean AssistantConversationService conversations;
    @MockitoBean RealtimeSessionService sessions;
    @MockitoBean RealtimeGateway gateway;
    @MockitoBean AssistantToolRegistry tools;
    @MockitoBean AgentOrchestrator agent;
    @MockitoBean AssistantPreferencesService preferences;
    @Test void agentHistoryAndPreferencesRequireAuthentication() throws Exception {
        mvc.perform(get("/api/v1/assistant/tasks")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/assistant/preferences")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/assistant/tasks").contentType("application/json").content("{}"))
            .andExpect(status().isUnauthorized());
        verifyNoInteractions(agent,preferences);
    }
    @Test void anonymousRequestsCannotCreateSessionsOrReadHistory() throws Exception {
        mvc.perform(post("/api/v1/assistant/sessions").contentType("application/json").content("{}"))
            .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/assistant/conversation").contentType("application/json").content("{}"))
            .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/assistant/config")).andExpect(status().isUnauthorized());
        verifyNoInteractions(sessions, conversations);
    }
    @Test void forgedTokenIsRejected() throws Exception {
        when(jwtService.parseAccessToken("forged")).thenThrow(new IllegalArgumentException("bad"));
        mvc.perform(post("/api/v1/assistant/sessions").header("Authorization", "Bearer forged")
            .contentType("application/json").content("{}"))
            .andExpect(status().isUnauthorized());
        verifyNoInteractions(sessions);
    }
    @Test void sessionUsesAuthenticatedIdentityAndNeverReturnsPermanentCredentials() throws Exception {
        var user = java.util.UUID.randomUUID(); var id = java.util.UUID.randomUUID(); var conversationId = java.util.UUID.randomUUID();
        when(jwtService.parseAccessToken("valid")).thenReturn(new com.abhiai.abhiai_backend.security.JwtPrincipal(user, "test@example.com"));
        when(sessions.create(user, id)).thenReturn(new RealtimeSessionService.SessionResponse(id, "auth_tokens/short-lived", "gemini-3.1-flash-live-preview", java.time.Instant.now().plusSeconds(900), 120));
        mvc.perform(post("/api/v1/assistant/sessions").header("Authorization", "Bearer valid")
                .contentType("application/json").content("{\"id\":\"" + id + "\",\"conversationId\":\"" + conversationId + "\"}"))
                .andExpect(status().isCreated()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.token").value("auth_tokens/short-lived"))
                .andExpect(jsonPath("$.apiKey").doesNotExist()).andExpect(jsonPath("$.client_secret").doesNotExist());
        verify(conversations).history(user, conversationId);
        verify(sessions).create(user, id);
    }

    @Test void toolsRequireAuthenticationAndOwnedAssistantConversation() throws Exception {
        mvc.perform(post("/api/v1/assistant/tools").contentType("application/json").content("{}"))
            .andExpect(status().isUnauthorized());
        var user=java.util.UUID.randomUUID();var conversation=java.util.UUID.randomUUID();
        when(jwtService.parseAccessToken("valid")).thenReturn(new com.abhiai.abhiai_backend.security.JwtPrincipal(user,"test@example.com"));
        when(conversations.history(user,conversation)).thenThrow(new com.abhiai.abhiai_backend.exception.ConversationNotFoundException());
        mvc.perform(post("/api/v1/assistant/tools").header("Authorization","Bearer valid").contentType("application/json")
            .content("{\"conversationId\":\""+conversation+"\",\"name\":\"SEARCH_ABHIAI\",\"arguments\":{\"query\":\"Java\"}}"))
            .andExpect(status().isNotFound());
        verifyNoInteractions(tools);
    }
    @Test void malformedToolContextIsRejectedBeforeExecution() throws Exception {
        var user=java.util.UUID.randomUUID();
        when(jwtService.parseAccessToken("valid")).thenReturn(new com.abhiai.abhiai_backend.security.JwtPrincipal(user,"test@example.com"));
        mvc.perform(post("/api/v1/assistant/tools").header("Authorization","Bearer valid").contentType("application/json")
            .content("{\"conversationId\":\""+java.util.UUID.randomUUID()+"\",\"name\":\"SEARCH_ABHIAI\",\"arguments\":{\"query\":\"Java\"},\"context\":{\"pageType\":\"password\",\"externalProcessingAllowed\":false}}"))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(tools);
    }

}
