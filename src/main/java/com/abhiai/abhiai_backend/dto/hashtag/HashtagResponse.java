package com.abhiai.abhiai_backend.dto.hashtag;

import java.time.Instant;
import java.util.UUID;

import com.abhiai.abhiai_backend.entity.Hashtag;

public record HashtagResponse(
        UUID id,
        String normalizedTag,
        String displayTag,
        long postCount,
        Instant createdAt) {
    public static HashtagResponse from(Hashtag hashtag) {
        return new HashtagResponse(
                hashtag.getId(), hashtag.getNormalizedTag(), hashtag.getDisplayTag(),
                hashtag.getPostCount(), hashtag.getCreatedAt());
    }
}
