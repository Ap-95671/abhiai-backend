package com.abhiai.abhiai_backend.dto.post;

import java.time.Instant;
import java.util.UUID;
import java.util.List;
import com.abhiai.abhiai_backend.dto.media.MediaAssetResponse;
import com.abhiai.abhiai_backend.dto.community.CommunitySummaryResponse;

import com.abhiai.abhiai_backend.entity.Post;
import com.abhiai.abhiai_backend.entity.PostVisibility;

public record PostResponse(
        UUID id,
        PostAuthorResponse author,
        String textContent,
        PostVisibility visibility,
        long replyCount,
        long likeCount,
        long repostCount,
        long bookmarkCount,
        long viewCount,
        boolean pinned,
        Instant createdAt,
        Instant updatedAt,
        List<MediaAssetResponse> media,
        CommunitySummaryResponse community,
        PollResponse poll) {

    public static PostResponse from(Post post) {
        return new PostResponse(
                post.getId(),
                PostAuthorResponse.from(post.getAuthor()),
                post.getTextContent(),
                post.getVisibility(),
                post.getReplyCount(),
                post.getLikeCount(),
                post.getRepostCount(),
                post.getBookmarkCount(),
                post.getViewCount(),
                post.getPinnedAt() != null,
                post.getCreatedAt(),
                post.getUpdatedAt(),
                post.getMedia().stream().map(MediaAssetResponse::from).toList(),
                post.getCommunity() == null
                        ? null
                        : CommunitySummaryResponse.from(post.getCommunity()),
                post.getPoll() == null ? null : PollResponse.from(post.getPoll(), null));
    }
}
