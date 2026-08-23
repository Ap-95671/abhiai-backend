package com.abhiai.abhiai_backend.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.abhiai.abhiai_backend.dto.common.PageResponse;
import com.abhiai.abhiai_backend.dto.hashtag.HashtagResponse;
import com.abhiai.abhiai_backend.dto.post.PostResponse;
import com.abhiai.abhiai_backend.security.JwtPrincipal;
import com.abhiai.abhiai_backend.service.HashtagService;

@RestController
@RequestMapping("/api/v1/hashtags")
public class HashtagController {

    private final HashtagService hashtagService;

    public HashtagController(HashtagService hashtagService) { this.hashtagService = hashtagService; }

    @GetMapping("/trending")
    public ResponseEntity<PageResponse<HashtagResponse>> trending(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(hashtagService.trending(pageable));
    }

    @GetMapping("/{tag}/posts")
    public ResponseEntity<PageResponse<PostResponse>> posts(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable String tag,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(hashtagService.posts(principal.userId(), tag, pageable));
    }
}
