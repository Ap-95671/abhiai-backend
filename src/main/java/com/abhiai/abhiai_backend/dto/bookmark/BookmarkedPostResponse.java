package com.abhiai.abhiai_backend.dto.bookmark;

import java.time.Instant;

import com.abhiai.abhiai_backend.dto.post.PostResponse;
import com.abhiai.abhiai_backend.entity.PostBookmark;

public record BookmarkedPostResponse(PostResponse post, Instant bookmarkedAt) {

    public static BookmarkedPostResponse from(PostBookmark bookmark) {
        return new BookmarkedPostResponse(
                PostResponse.from(bookmark.getPost()),
                bookmark.getCreatedAt());
    }
}
