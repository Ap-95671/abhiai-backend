package com.abhiai.abhiai_backend.controller;

import java.util.UUID;
import java.util.List;

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

import com.abhiai.abhiai_backend.dto.post.CreatePostRequest;
import com.abhiai.abhiai_backend.dto.post.PostResponse;
import com.abhiai.abhiai_backend.dto.post.UpdatePostRequest;
import com.abhiai.abhiai_backend.dto.mention.MentionResponse;
import com.abhiai.abhiai_backend.security.JwtPrincipal;
import com.abhiai.abhiai_backend.service.PostService;
import com.abhiai.abhiai_backend.service.MentionService;
import com.abhiai.abhiai_backend.service.PollService;
import com.abhiai.abhiai_backend.dto.post.CastPollVoteRequest;
import com.abhiai.abhiai_backend.dto.post.PollResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/posts")
public class PostController {

    private final PostService postService;
    private final MentionService mentionService;
    private final PollService pollService;

    public PostController(PostService postService, MentionService mentionService, PollService pollService) {
        this.postService = postService;
        this.mentionService = mentionService;
        this.pollService = pollService;
    }

    @PostMapping
    public ResponseEntity<PostResponse> createPost(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody CreatePostRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(postService.createPost(principal.userId(), request));
    }

    @GetMapping("/{postId}")
    public ResponseEntity<PostResponse> getPost(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID postId) {
        return ResponseEntity.ok(postService.getPost(principal.userId(), postId));
    }

    @GetMapping("/{postId}/mentions")
    public ResponseEntity<List<MentionResponse>> getPostMentions(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID postId) {
        return ResponseEntity.ok(mentionService.getPostMentions(principal.userId(), postId));
    }

    @PatchMapping("/{postId}")
    public ResponseEntity<PostResponse> updatePost(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID postId,
            @Valid @RequestBody UpdatePostRequest request) {
        return ResponseEntity.ok(postService.updatePost(principal.userId(), postId, request));
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID postId) {
        postService.deletePost(principal.userId(), postId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{postId}/pin")
    public ResponseEntity<PostResponse> pinPost(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID postId) {
        return ResponseEntity.ok(postService.pinPost(principal.userId(), postId));
    }

    @DeleteMapping("/{postId}/pin")
    public ResponseEntity<Void> unpinPost(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID postId) {
        postService.unpinPost(principal.userId(), postId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{postId}/poll")
    public ResponseEntity<PollResponse> getPoll(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID postId) {
        return ResponseEntity.ok(pollService.getPoll(principal.userId(), postId));
    }

    @PostMapping("/{postId}/poll/votes")
    public ResponseEntity<PollResponse> vote(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID postId,
            @Valid @RequestBody CastPollVoteRequest request) {
        return ResponseEntity.ok(pollService.vote(principal.userId(), postId, request.choiceId()));
    }
}
