package com.abhiai.abhiai_backend.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.abhiai.abhiai_backend.dto.chat.ChatExchangeResponse;
import com.abhiai.abhiai_backend.dto.chat.ConversationDetailResponse;
import com.abhiai.abhiai_backend.dto.chat.ConversationSummaryResponse;
import com.abhiai.abhiai_backend.dto.chat.CreateConversationRequest;
import com.abhiai.abhiai_backend.dto.chat.RenameConversationRequest;
import com.abhiai.abhiai_backend.dto.chat.SendMessageRequest;
import com.abhiai.abhiai_backend.dto.chat.UpdateModelPreferenceRequest;
import com.abhiai.abhiai_backend.exception.AiProviderException;
import com.abhiai.abhiai_backend.security.JwtPrincipal;
import com.abhiai.abhiai_backend.service.ChatService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/conversations")
public class ChatController {

    private final ChatService chatService;
    private final Executor aiStreamingExecutor;

    public ChatController(
            ChatService chatService,
            @Qualifier("aiStreamingExecutor") Executor aiStreamingExecutor) {
        this.chatService = chatService;
        this.aiStreamingExecutor = aiStreamingExecutor;
    }

    @PostMapping
    public ResponseEntity<ConversationSummaryResponse> createConversation(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody CreateConversationRequest request) {
        ConversationSummaryResponse response = chatService.createConversation(principal.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ConversationSummaryResponse>> getConversations(
            @AuthenticationPrincipal JwtPrincipal principal) {
        return ResponseEntity.ok(chatService.getConversations(principal.userId()));
    }

    @GetMapping("/{conversationId}")
    public ResponseEntity<ConversationDetailResponse> getConversationHistory(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID conversationId) {
        return ResponseEntity.ok(chatService.getConversationHistory(principal.userId(), conversationId));
    }

    @PatchMapping("/{conversationId}")
    public ResponseEntity<ConversationSummaryResponse> renameConversation(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID conversationId,
            @Valid @RequestBody RenameConversationRequest request) {
        return ResponseEntity.ok(chatService.renameConversation(principal.userId(), conversationId, request));
    }

    @PatchMapping("/{conversationId}/model")
    public ResponseEntity<ConversationSummaryResponse> updateModelPreference(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID conversationId,
            @Valid @RequestBody UpdateModelPreferenceRequest request) {
        return ResponseEntity.ok(chatService.updateModelPreference(principal.userId(), conversationId, request));
    }

    @DeleteMapping("/{conversationId}")
    public ResponseEntity<Void> deleteConversation(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID conversationId) {
        chatService.deleteConversation(principal.userId(), conversationId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{conversationId}/messages")
    public ResponseEntity<ChatExchangeResponse> addUserMessage(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID conversationId,
            @Valid @RequestBody SendMessageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(chatService.addUserMessage(principal.userId(), conversationId, request));
    }

    @PostMapping(value = "/{conversationId}/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamUserMessage(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID conversationId,
            @Valid @RequestBody SendMessageRequest request) {
        SseEmitter emitter = new SseEmitter(120_000L);
        AtomicReference<Thread> streamThread = new AtomicReference<>();
        AtomicBoolean streamFinished = new AtomicBoolean(false);
        Runnable cancelStream = () -> {
            Thread worker = streamThread.get();
            if (!streamFinished.get() && worker != null) {
                worker.interrupt();
            }
        };
        emitter.onCompletion(cancelStream);
        emitter.onTimeout(cancelStream);

        CompletableFuture.runAsync(() -> {
            streamThread.set(Thread.currentThread());
            try {
                ChatExchangeResponse exchange = chatService.addUserMessageStreaming(
                        principal.userId(),
                        conversationId,
                        request,
                        chunk -> sendEvent(emitter, "chunk", chunk));
                sendEvent(emitter, "complete", exchange);
                streamFinished.set(true);
                emitter.complete();
            } catch (Exception exception) {
                completeWithSafeError(emitter, exception);
            }
        }, aiStreamingExecutor);

        return emitter;
    }

    private void completeWithSafeError(SseEmitter emitter, Exception exception) {
        try {
            sendEvent(emitter, "error", Map.of("message", safeMessage(exception)));
            emitter.complete();
        } catch (Exception sendException) {
            emitter.completeWithError(sendException);
        }
    }

    private void sendEvent(SseEmitter emitter, String name, Object data) {
        try {
            emitter.send(SseEmitter.event().name(name).data(data));
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to send chat stream event", exception);
        }
    }

    private String safeMessage(Exception exception) {
        return exception instanceof AiProviderException || exception instanceof com.abhiai.abhiai_backend.exception.ModelRoutingException
                ? exception.getMessage()
                : "Chat generation failed";
    }
}
