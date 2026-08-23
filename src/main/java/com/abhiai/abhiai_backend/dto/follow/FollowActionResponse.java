package com.abhiai.abhiai_backend.dto.follow;

import java.time.Instant;
import java.util.UUID;

import com.abhiai.abhiai_backend.entity.Follow;

public record FollowActionResponse(
        UUID userId,
        boolean following,
        Instant followedAt) {

    public static FollowActionResponse from(Follow follow) {
        return new FollowActionResponse(
                follow.getFollowing().getId(),
                true,
                follow.getCreatedAt());
    }
}
