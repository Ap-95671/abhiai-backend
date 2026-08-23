package com.abhiai.abhiai_backend.dto.bookmark;

import java.time.Instant;
import java.util.UUID;

import com.abhiai.abhiai_backend.entity.PostBookmark;

public record PostBookmarkResponse(
        UUID postId,
        boolean bookmarked,
        Instant bookmarkedAt) {

    public static PostBookmarkResponse from(PostBookmark bookmark) {
        return new PostBookmarkResponse(
                bookmark.getPost().getId(),
                true,
                bookmark.getCreatedAt());
    }
}
