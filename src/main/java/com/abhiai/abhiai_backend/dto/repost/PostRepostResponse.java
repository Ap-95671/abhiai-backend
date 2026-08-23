package com.abhiai.abhiai_backend.dto.repost;

import java.time.Instant;
import java.util.UUID;

import com.abhiai.abhiai_backend.entity.PostRepost;

public record PostRepostResponse(
        UUID postId,
        UUID userId,
        boolean reposted,
        Instant repostedAt) {

    public static PostRepostResponse from(PostRepost repost) {
        return new PostRepostResponse(
                repost.getPost().getId(),
                repost.getUser().getId(),
                true,
                repost.getCreatedAt());
    }
}
