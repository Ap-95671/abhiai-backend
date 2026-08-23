package com.abhiai.abhiai_backend.controller;

import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.abhiai.abhiai_backend.dto.common.PageResponse;
import com.abhiai.abhiai_backend.dto.reply.CreateReplyRequest;
import com.abhiai.abhiai_backend.dto.reply.PostReplyResponse;
import com.abhiai.abhiai_backend.security.JwtPrincipal;
import com.abhiai.abhiai_backend.service.PostReplyService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/posts/{postId}/replies")
public class PostReplyController {

    private final PostReplyService postReplyService;

    public PostReplyController(PostReplyService postReplyService) {
        this.postReplyService = postReplyService;
    }

    @PostMapping
    public ResponseEntity<PostReplyResponse> createReply(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID postId,
            @Valid @RequestBody CreateReplyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(postReplyService.createReply(principal.userId(), postId, request));
    }

    @GetMapping
    public ResponseEntity<PageResponse<PostReplyResponse>> getReplies(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID postId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(postReplyService.getReplies(principal.userId(), postId, pageable));
    }

    @DeleteMapping("/{replyId}")
    public ResponseEntity<Void> deleteReply(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID postId,
            @PathVariable UUID replyId) {
        postReplyService.deleteReply(principal.userId(), postId, replyId);
        return ResponseEntity.noContent().build();
    }
}
