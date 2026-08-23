package com.abhiai.abhiai_backend.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.abhiai.abhiai_backend.dto.bookmark.PostBookmarkResponse;
import com.abhiai.abhiai_backend.dto.bookmark.PostBookmarkStatusResponse;
import com.abhiai.abhiai_backend.security.JwtPrincipal;
import com.abhiai.abhiai_backend.service.PostBookmarkService;

@RestController
@RequestMapping("/api/v1/posts/{postId}/bookmarks")
public class PostBookmarkController {

    private final PostBookmarkService postBookmarkService;

    public PostBookmarkController(PostBookmarkService postBookmarkService) {
        this.postBookmarkService = postBookmarkService;
    }

    @PostMapping
    public ResponseEntity<PostBookmarkResponse> bookmark(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID postId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(postBookmarkService.bookmark(principal.userId(), postId));
    }

    @DeleteMapping
    public ResponseEntity<Void> removeBookmark(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID postId) {
        postBookmarkService.removeBookmark(principal.userId(), postId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/status")
    public ResponseEntity<PostBookmarkStatusResponse> getStatus(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID postId) {
        return ResponseEntity.ok(postBookmarkService.getStatus(principal.userId(), postId));
    }
}
