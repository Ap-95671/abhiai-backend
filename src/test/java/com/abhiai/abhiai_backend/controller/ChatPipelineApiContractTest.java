package com.abhiai.abhiai_backend.controller;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.*;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.abhiai.abhiai_backend.dto.chat.*;
import com.abhiai.abhiai_backend.entity.*;
import com.abhiai.abhiai_backend.security.JwtPrincipal;
import com.abhiai.abhiai_backend.service.ChatService;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ChatPipelineApiContractTest {
    @Test void normalPostAndSseKeepExistingUrlsRequestAndResponseShape() throws Exception {
        UUID userId = UUID.randomUUID(), conversationId = UUID.randomUUID();
        var service = mock(ChatService.class);
        var conversation = new Conversation(new User("user", "User", "user@example.com", "hash"), "Chat");
        var exchange = new ChatExchangeResponse(MessageResponse.from(new Message(conversation, MessageRole.USER, "Hello")),
                MessageResponse.from(new Message(conversation, MessageRole.ASSISTANT, "Final answer")),
                ConversationSummaryResponse.from(conversation));
        when(service.addUserMessage(eq(userId), eq(conversationId), any())).thenReturn(exchange);
        when(service.addUserMessageStreaming(eq(userId), eq(conversationId), any(), any(), any())).thenAnswer(call -> {
            java.util.function.Consumer<String> chunks = call.getArgument(3);
            chunks.accept("Final answer");
            return exchange;
        });
        var mvc = MockMvcBuilders.standaloneSetup(new ChatController(service, Runnable::run))
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    public boolean supportsParameter(MethodParameter parameter) { return parameter.getParameterType() == JwtPrincipal.class; }
                    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer container, NativeWebRequest request,
                                                  WebDataBinderFactory factory) { return new JwtPrincipal(userId, "user@example.com"); }
                }).build();
        String path = "/api/v1/conversations/" + conversationId + "/messages";
        mvc.perform(post(path).contentType("application/json").content("{\"content\":\"Hello\",\"externalProcessingAllowed\":false,\"webSearchAllowed\":false}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.userMessage.content").value("Hello"))
                .andExpect(jsonPath("$.assistantMessage.content").value("Final answer"))
                .andExpect(jsonPath("$.assistantMessage.attachments").isArray())
                .andExpect(jsonPath("$.conversation").exists());
        var streaming = mvc.perform(post(path + "/stream").contentType("application/json").content("{\"content\":\"Hello\",\"externalProcessingAllowed\":false,\"webSearchAllowed\":false}"))
                .andExpect(request().asyncStarted()).andReturn();
        mvc.perform(asyncDispatch(streaming)).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("event:chunk")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("event:complete")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("assistantMessage")));
    }
    @Test void streamingWorkerReusesRequestIdAndClearsItAfterSearchFailure() throws Exception {
        var service = mock(ChatService.class);
        var observed = new java.util.concurrent.atomic.AtomicReference<String>();
        when(service.addUserMessageStreaming(any(), any(), any(), any(), any())).thenAnswer(call -> {
            observed.set(org.slf4j.MDC.get("requestId"));
            SendMessageRequest request = call.getArgument(2);
            org.assertj.core.api.Assertions.assertThat(request.webSearchAllowed()).isTrue();
            throw new com.abhiai.abhiai_backend.exception.AiProviderException("Current information could not be verified.",
                    com.abhiai.abhiai_backend.exception.AiProviderFailureKind.SEARCH_FAILURE);
        });
        var worker = java.util.concurrent.Executors.newSingleThreadExecutor();
        try {
            org.slf4j.MDC.put("requestId", "search-trace-123");
            new ChatController(service, worker).streamUserMessage(new JwtPrincipal(UUID.randomUUID(), "user@example.com"),
                    UUID.randomUUID(), new SendMessageRequest("Explain merge sort", List.of(), false, true, null, null, null));
            org.assertj.core.api.Assertions.assertThat(worker.submit(() -> org.slf4j.MDC.get("requestId")).get(5, java.util.concurrent.TimeUnit.SECONDS)).isNull();
            org.assertj.core.api.Assertions.assertThat(observed.get()).isEqualTo("search-trace-123");
        } finally { org.slf4j.MDC.clear(); worker.shutdownNow(); }
    }

}
