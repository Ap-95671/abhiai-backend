package com.abhiai.abhiai_backend.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.abhiai.abhiai_backend.dto.chat.ConversationAttachmentResponse;
import com.abhiai.abhiai_backend.security.JwtPrincipal;
import com.abhiai.abhiai_backend.service.ConversationAttachmentService;

@RestController
@RequestMapping("/api/v1/conversations/{conversationId}/attachments")
public class ConversationAttachmentController {

    private final ConversationAttachmentService service;

    public ConversationAttachmentController(ConversationAttachmentService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ConversationAttachmentResponse> upload(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID conversationId,
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.upload(principal.userId(), conversationId, file));
    }

    @GetMapping
    public List<ConversationAttachmentResponse> list(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID conversationId) {
        return service.list(principal.userId(), conversationId);
    }

    @DeleteMapping("/{attachmentId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID conversationId,
            @PathVariable UUID attachmentId) {
        service.delete(principal.userId(), conversationId, attachmentId);
        return ResponseEntity.noContent().build();
    }
}
