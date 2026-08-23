package com.abhiai.abhiai_backend.controller;

import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.abhiai.abhiai_backend.dto.common.PageResponse;
import com.abhiai.abhiai_backend.dto.post.PostResponse;
import com.abhiai.abhiai_backend.dto.post.PostViewResponse;
import com.abhiai.abhiai_backend.security.JwtPrincipal;
import com.abhiai.abhiai_backend.service.VideoFeedService;

@RestController
@RequestMapping("/api/v1/videos")
public class VideoController {

    private final VideoFeedService videoFeedService;

    public VideoController(VideoFeedService videoFeedService) {
        this.videoFeedService = videoFeedService;
    }

    @GetMapping("/feed")
    public ResponseEntity<PageResponse<PostResponse>> getVideoFeed(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(videoFeedService.getVideoFeed(principal.userId(), pageable));
    }

    @PostMapping("/{postId}/views")
    public ResponseEntity<PostViewResponse> recordView(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID postId) {
        return ResponseEntity.ok(videoFeedService.recordView(principal.userId(), postId));
    }
}
