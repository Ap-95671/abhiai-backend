package com.abhiai.abhiai_backend.dto.like;

import java.time.Instant;
import java.util.UUID;

import com.abhiai.abhiai_backend.entity.PostLike;

public record PostLikeResponse(
        UUID postId,
        UUID userId,
        boolean liked,
        Instant likedAt) {

    public static PostLikeResponse from(PostLike like) {
        return new PostLikeResponse(
                like.getPost().getId(),
                like.getUser().getId(),
                true,
                like.getCreatedAt());
    }
}
