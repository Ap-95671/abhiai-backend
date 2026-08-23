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
import com.abhiai.abhiai_backend.dto.story.CreateStoryRequest;
import com.abhiai.abhiai_backend.dto.story.StoryReactionRequest;
import com.abhiai.abhiai_backend.dto.story.StoryReactionResponse;
import com.abhiai.abhiai_backend.dto.story.StoryResponse;
import com.abhiai.abhiai_backend.dto.story.StoryViewResponse;
import com.abhiai.abhiai_backend.security.JwtPrincipal;
import com.abhiai.abhiai_backend.service.StoryService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/stories")
public class StoryController {

    private final StoryService storyService;

    public StoryController(StoryService storyService) { this.storyService = storyService; }

    @PostMapping
    public ResponseEntity<StoryResponse> create(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody CreateStoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(storyService.create(principal.userId(), request));
    }

    @GetMapping("/feed")
    public ResponseEntity<PageResponse<StoryResponse>> feed(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(storyService.getFeed(principal.userId(), pageable));
    }

    @GetMapping("/{storyId}")
    public ResponseEntity<StoryResponse> get(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID storyId) {
        return ResponseEntity.ok(storyService.get(principal.userId(), storyId));
    }

    @PostMapping("/{storyId}/views")
    public ResponseEntity<StoryViewResponse> view(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID storyId) {
        return ResponseEntity.ok(storyService.recordView(principal.userId(), storyId));
    }

    @PostMapping("/{storyId}/reaction")
    public ResponseEntity<StoryReactionResponse> react(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID storyId,
            @Valid @RequestBody StoryReactionRequest request) {
        return ResponseEntity.ok(storyService.react(principal.userId(), storyId, request.reaction()));
    }

    @DeleteMapping("/{storyId}/reaction")
    public ResponseEntity<StoryReactionResponse> removeReaction(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID storyId) {
        return ResponseEntity.ok(storyService.removeReaction(principal.userId(), storyId));
    }

    @DeleteMapping("/{storyId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID storyId) {
        storyService.delete(principal.userId(), storyId);
        return ResponseEntity.noContent().build();
    }
}
