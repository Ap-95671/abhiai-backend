package com.abhiai.abhiai_backend.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
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

import com.abhiai.abhiai_backend.dto.common.PageResponse;
import com.abhiai.abhiai_backend.dto.directmessage.CreateDirectConversationRequest;
import com.abhiai.abhiai_backend.dto.directmessage.DirectConversationResponse;
import com.abhiai.abhiai_backend.dto.directmessage.DirectMessageReadResponse;
import com.abhiai.abhiai_backend.dto.directmessage.DirectMessageResponse;
import com.abhiai.abhiai_backend.dto.directmessage.SendDirectMessageRequest;
import com.abhiai.abhiai_backend.security.JwtPrincipal;
import com.abhiai.abhiai_backend.service.DirectMessagingService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/direct-messages")
public class DirectMessagingController {

    private final DirectMessagingService messagingService;

    public DirectMessagingController(DirectMessagingService messagingService) {
        this.messagingService = messagingService;
    }

    @PostMapping("/conversations")
    public ResponseEntity<DirectConversationResponse> startConversation(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody CreateDirectConversationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                messagingService.startConversation(principal.userId(), request.recipientUsername()));
    }

    @GetMapping("/conversations")
    public ResponseEntity<List<DirectConversationResponse>> getConversations(
            @AuthenticationPrincipal JwtPrincipal principal) {
        return ResponseEntity.ok(messagingService.getConversations(principal.userId()));
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<PageResponse<DirectMessageResponse>> getHistory(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID conversationId,
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(
                messagingService.getHistory(principal.userId(), conversationId, pageable));
    }

    @PostMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<DirectMessageResponse> sendMessage(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID conversationId,
            @Valid @RequestBody SendDirectMessageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                messagingService.sendMessage(principal.userId(), conversationId, request.content()));
    }

    @PatchMapping("/conversations/{conversationId}/read")
    public ResponseEntity<DirectMessageReadResponse> markRead(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID conversationId) {
        return ResponseEntity.ok(
                messagingService.markConversationRead(principal.userId(), conversationId));
    }

    @DeleteMapping("/conversations/{conversationId}/messages/{messageId}")
    public ResponseEntity<DirectMessageResponse> deleteMessage(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID conversationId,
            @PathVariable UUID messageId) {
        return ResponseEntity.ok(
                messagingService.deleteMessage(principal.userId(), conversationId, messageId));
    }
}
