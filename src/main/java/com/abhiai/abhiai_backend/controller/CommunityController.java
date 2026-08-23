package com.abhiai.abhiai_backend.controller;

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
import com.abhiai.abhiai_backend.dto.community.CommunityResponse;
import com.abhiai.abhiai_backend.dto.community.CreateCommunityPostRequest;
import com.abhiai.abhiai_backend.dto.community.CreateCommunityRequest;
import com.abhiai.abhiai_backend.dto.post.PostResponse;
import com.abhiai.abhiai_backend.security.JwtPrincipal;
import com.abhiai.abhiai_backend.service.CommunityService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/communities")
public class CommunityController {

    private final CommunityService communityService;

    public CommunityController(CommunityService communityService) {
        this.communityService = communityService;
    }

    @PostMapping
    public ResponseEntity<CommunityResponse> createCommunity(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody CreateCommunityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(communityService.createCommunity(principal.userId(), request));
    }

    @GetMapping
    public ResponseEntity<PageResponse<CommunityResponse>> getCommunities(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PageableDefault(size = 24) Pageable pageable) {
        return ResponseEntity.ok(communityService.getCommunities(principal.userId(), pageable));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<CommunityResponse> getCommunity(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable String slug) {
        return ResponseEntity.ok(communityService.getCommunity(principal.userId(), slug));
    }

    @PostMapping("/{slug}/membership")
    public ResponseEntity<CommunityResponse> joinCommunity(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable String slug) {
        return ResponseEntity.ok(communityService.joinCommunity(principal.userId(), slug));
    }

    @DeleteMapping("/{slug}/membership")
    public ResponseEntity<CommunityResponse> leaveCommunity(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable String slug) {
        return ResponseEntity.ok(communityService.leaveCommunity(principal.userId(), slug));
    }

    @GetMapping("/{slug}/posts")
    public ResponseEntity<PageResponse<PostResponse>> getCommunityFeed(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable String slug,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(
                communityService.getCommunityFeed(principal.userId(), slug, pageable));
    }

    @PostMapping("/{slug}/posts")
    public ResponseEntity<PostResponse> publishPost(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable String slug,
            @Valid @RequestBody CreateCommunityPostRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(communityService.publishPost(principal.userId(), slug, request));
    }
}
