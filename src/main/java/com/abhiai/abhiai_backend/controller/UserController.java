package com.abhiai.abhiai_backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.abhiai.abhiai_backend.dto.user.UpdateUserProfileRequest;
import com.abhiai.abhiai_backend.dto.user.UserProfileResponse;
import com.abhiai.abhiai_backend.dto.common.PageResponse;
import com.abhiai.abhiai_backend.dto.follow.FollowActionResponse;
import com.abhiai.abhiai_backend.dto.follow.FollowStatusResponse;
import com.abhiai.abhiai_backend.dto.follow.FollowUserResponse;
import com.abhiai.abhiai_backend.security.JwtPrincipal;
import com.abhiai.abhiai_backend.service.FollowService;
import com.abhiai.abhiai_backend.service.UserProfileService;
import com.abhiai.abhiai_backend.service.ProfileContentService;
import com.abhiai.abhiai_backend.service.UserBlockService;
import com.abhiai.abhiai_backend.dto.user.BlockStatusResponse;
import com.abhiai.abhiai_backend.dto.user.BlockedUserResponse;
import com.abhiai.abhiai_backend.dto.user.AccountPrivacyRequest;
import com.abhiai.abhiai_backend.dto.post.PostResponse;
import com.abhiai.abhiai_backend.dto.user.ProfileReplyResponse;

import jakarta.validation.Valid;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserProfileService userProfileService;
    private final FollowService followService;
    private final ProfileContentService profileContentService;
    private final UserBlockService userBlockService;

    public UserController(
            UserProfileService userProfileService,
            FollowService followService,
            ProfileContentService profileContentService,
            UserBlockService userBlockService) {
        this.userProfileService = userProfileService;
        this.followService = followService;
        this.profileContentService = profileContentService;
        this.userBlockService = userBlockService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUser(
            @AuthenticationPrincipal JwtPrincipal principal) {
        return ResponseEntity.ok(userProfileService.getCurrentUser(principal.userId()));
    }

    @PatchMapping("/me/profile")
    public ResponseEntity<UserProfileResponse> updateCurrentUser(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody UpdateUserProfileRequest request) {
        return ResponseEntity.ok(userProfileService.updateCurrentUser(principal.userId(), request));
    }
    @PatchMapping("/me/privacy") public ResponseEntity<UserProfileResponse> privacy(@AuthenticationPrincipal JwtPrincipal principal,@Valid @RequestBody AccountPrivacyRequest request){return ResponseEntity.ok(userProfileService.updatePrivacy(principal.userId(),request.privacy()));}

    @GetMapping("/{username}")
    public ResponseEntity<UserProfileResponse> getByUsername(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable String username) {
        return ResponseEntity.ok(userProfileService.getByUsername(principal.userId(), username));
    }

    @PostMapping("/{userId}/block")
    public ResponseEntity<BlockStatusResponse> block(@AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID userId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userBlockService.block(principal.userId(), userId));
    }

    @DeleteMapping("/{userId}/block")
    public ResponseEntity<BlockStatusResponse> unblock(@AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID userId) {
        return ResponseEntity.ok(userBlockService.unblock(principal.userId(), userId));
    }

    @GetMapping("/{userId}/block-status")
    public ResponseEntity<BlockStatusResponse> blockStatus(@AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID userId) {
        return ResponseEntity.ok(userBlockService.status(principal.userId(), userId));
    }

    @GetMapping("/me/blocks")
    public ResponseEntity<PageResponse<BlockedUserResponse>> blocks(@AuthenticationPrincipal JwtPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(userBlockService.list(principal.userId(), pageable));
    }

    @GetMapping("/{username}/posts")
    public ResponseEntity<PageResponse<PostResponse>> getProfilePosts(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable String username,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(profileContentService.getPosts(principal.userId(), username, pageable));
    }

    @GetMapping("/{username}/replies")
    public ResponseEntity<PageResponse<ProfileReplyResponse>> getProfileReplies(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable String username,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(profileContentService.getReplies(principal.userId(), username, pageable));
    }

    @GetMapping("/{username}/media")
    public ResponseEntity<PageResponse<PostResponse>> getProfileMedia(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable String username,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(profileContentService.getMedia(principal.userId(), username, pageable));
    }

    @GetMapping("/{username}/likes")
    public ResponseEntity<PageResponse<PostResponse>> getProfileLikes(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable String username,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(profileContentService.getLikes(principal.userId(), username, pageable));
    }

    @PostMapping("/{userId}/follow")
    public ResponseEntity<FollowActionResponse> follow(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(followService.follow(principal.userId(), userId));
    }

    @DeleteMapping("/{userId}/follow")
    public ResponseEntity<Void> unfollow(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID userId) {
        followService.unfollow(principal.userId(), userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{userId}/follow-status")
    public ResponseEntity<FollowStatusResponse> getFollowStatus(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID userId) {
        return ResponseEntity.ok(followService.getFollowStatus(principal.userId(), userId));
    }

    @GetMapping("/{userId}/followers")
    public ResponseEntity<PageResponse<FollowUserResponse>> getFollowers(
            @PathVariable UUID userId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(followService.getFollowers(userId, pageable));
    }

    @GetMapping("/{userId}/following")
    public ResponseEntity<PageResponse<FollowUserResponse>> getFollowing(
            @PathVariable UUID userId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(followService.getFollowing(userId, pageable));
    }
}
