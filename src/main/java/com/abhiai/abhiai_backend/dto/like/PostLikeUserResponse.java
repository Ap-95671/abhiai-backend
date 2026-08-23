package com.abhiai.abhiai_backend.dto.like;

import java.time.Instant;
import java.util.UUID;

import com.abhiai.abhiai_backend.entity.PostLike;
import com.abhiai.abhiai_backend.entity.User;
import com.abhiai.abhiai_backend.entity.VerifiedStatus;

public record PostLikeUserResponse(
        UUID id,
        String username,
        String displayName,
        String profilePicture,
        VerifiedStatus verifiedStatus,
        Instant likedAt) {

    public static PostLikeUserResponse from(PostLike like) {
        User user = like.getUser();
        return new PostLikeUserResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getProfilePicture(),
                user.getVerifiedStatus(),
                like.getCreatedAt());
    }
}
