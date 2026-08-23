package com.abhiai.abhiai_backend.dto.follow;

import java.time.Instant;
import java.util.UUID;

import com.abhiai.abhiai_backend.entity.User;
import com.abhiai.abhiai_backend.entity.VerifiedStatus;

public record FollowUserResponse(
        UUID id,
        String username,
        String displayName,
        String profilePicture,
        VerifiedStatus verifiedStatus,
        long followerCount,
        long followingCount,
        Instant followedAt) {

    public static FollowUserResponse from(User user, Instant followedAt) {
        return new FollowUserResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getProfilePicture(),
                user.getVerifiedStatus(),
                user.getFollowerCount(),
                user.getFollowingCount(),
                followedAt);
    }
}
