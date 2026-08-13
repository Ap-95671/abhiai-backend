package com.abhiai.abhiai_backend.controller;

import org.springframework.web.bind.annotation.DeleteMapping;

import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.PatchMapping;

import com.abhiai.abhiai_backend.dto.chat.RenameConversationRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.abhiai.abhiai_backend.dto.chat.ChatExchangeResponse;
import com.abhiai.abhiai_backend.dto.chat.ConversationDetailResponse;
import com.abhiai.abhiai_backend.dto.chat.ConversationSummaryResponse;
import com.abhiai.abhiai_backend.dto.chat.CreateConversationRequest;
import com.abhiai.abhiai_backend.dto.chat.SendMessageRequest;
import com.abhiai.abhiai_backend.security.JwtPrincipal;
import com.abhiai.abhiai_backend.service.ChatService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/conversations")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }
    @PatchMapping("/{conversationId}")
public ResponseEntity<ConversationSummaryResponse> renameConversation(
        @AuthenticationPrincipal JwtPrincipal principal,
        @PathVariable UUID conversationId,
        @Valid @RequestBody RenameConversationRequest request) {

    return ResponseEntity.ok(
            chatService.renameConversation(
                    principal.userId(),
                    conversationId,
                    request));
}
@DeleteMapping("/{conversationId}")
public ResponseEntity<Void> deleteConversation(
        @AuthenticationPrincipal JwtPrincipal principal,
        @PathVariable UUID conversationId) {

    chatService.deleteConversation(
            principal.userId(),
            conversationId);

    return ResponseEntity.noContent().build();
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

    @PostMapping("/{conversationId}/messages")
    public ResponseEntity<ChatExchangeResponse> addUserMessage(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID conversationId,
            @Valid @RequestBody SendMessageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(chatService.addUserMessage(principal.userId(), conversationId, request));
    }
}
