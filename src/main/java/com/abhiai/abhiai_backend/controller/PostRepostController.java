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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.abhiai.abhiai_backend.dto.common.PageResponse;
import com.abhiai.abhiai_backend.dto.repost.PostRepostResponse;
import com.abhiai.abhiai_backend.dto.repost.PostRepostStatusResponse;
import com.abhiai.abhiai_backend.dto.repost.PostRepostUserResponse;
import com.abhiai.abhiai_backend.security.JwtPrincipal;
import com.abhiai.abhiai_backend.service.PostRepostService;

@RestController
@RequestMapping("/api/v1/posts/{postId}/reposts")
public class PostRepostController {

    private final PostRepostService postRepostService;

    public PostRepostController(PostRepostService postRepostService) {
        this.postRepostService = postRepostService;
    }

    @PostMapping
    public ResponseEntity<PostRepostResponse> repost(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID postId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(postRepostService.repost(principal.userId(), postId));
    }

    @DeleteMapping
    public ResponseEntity<Void> removeRepost(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID postId) {
        postRepostService.removeRepost(principal.userId(), postId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/status")
    public ResponseEntity<PostRepostStatusResponse> getStatus(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID postId) {
        return ResponseEntity.ok(postRepostService.getStatus(principal.userId(), postId));
    }

    @GetMapping
    public ResponseEntity<PageResponse<PostRepostUserResponse>> getReposts(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID postId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(postRepostService.getReposts(principal.userId(), postId, pageable));
    }
}
