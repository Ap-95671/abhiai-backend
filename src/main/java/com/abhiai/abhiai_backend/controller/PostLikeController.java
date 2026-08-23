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
import com.abhiai.abhiai_backend.dto.like.PostLikeResponse;
import com.abhiai.abhiai_backend.dto.like.PostLikeStatusResponse;
import com.abhiai.abhiai_backend.dto.like.PostLikeUserResponse;
import com.abhiai.abhiai_backend.security.JwtPrincipal;
import com.abhiai.abhiai_backend.service.PostLikeService;

@RestController
@RequestMapping("/api/v1/posts/{postId}/likes")
public class PostLikeController {

    private final PostLikeService postLikeService;

    public PostLikeController(PostLikeService postLikeService) {
        this.postLikeService = postLikeService;
    }

    @PostMapping
    public ResponseEntity<PostLikeResponse> like(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID postId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(postLikeService.like(principal.userId(), postId));
    }

    @DeleteMapping
    public ResponseEntity<Void> unlike(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID postId) {
        postLikeService.unlike(principal.userId(), postId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/status")
    public ResponseEntity<PostLikeStatusResponse> getStatus(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID postId) {
        return ResponseEntity.ok(postLikeService.getStatus(principal.userId(), postId));
    }

    @GetMapping
    public ResponseEntity<PageResponse<PostLikeUserResponse>> getLikes(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID postId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(postLikeService.getLikes(principal.userId(), postId, pageable));
    }
}
