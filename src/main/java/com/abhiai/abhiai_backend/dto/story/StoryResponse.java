package com.abhiai.abhiai_backend.dto.story;

import java.time.Instant;
import java.util.UUID;

import com.abhiai.abhiai_backend.dto.media.MediaAssetResponse;
import com.abhiai.abhiai_backend.dto.post.PostAuthorResponse;
import com.abhiai.abhiai_backend.entity.Story;
import com.abhiai.abhiai_backend.entity.StoryType;

public record StoryResponse(
        UUID id,
        PostAuthorResponse author,
        StoryType type,
        String textContent,
        String backgroundColor,
        MediaAssetResponse media,
        long viewCount,
        long reactionCount,
        boolean viewedByCurrentUser,
        String currentUserReaction,
        Instant expiresAt,
        Instant createdAt) {

    public static StoryResponse from(Story story, boolean viewed, String reaction) {
        return new StoryResponse(
                story.getId(),
                PostAuthorResponse.from(story.getAuthor()),
                story.getType(),
                story.getTextContent(),
                story.getBackgroundColor(),
                story.getMedia() == null ? null : MediaAssetResponse.from(story.getMedia()),
                story.getViewCount(),
                story.getReactionCount(),
                viewed,
                reaction,
                story.getExpiresAt(),
                story.getCreatedAt());
    }
}
