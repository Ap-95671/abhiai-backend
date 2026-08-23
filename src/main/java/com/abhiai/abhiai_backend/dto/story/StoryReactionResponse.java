package com.abhiai.abhiai_backend.dto.story;

import java.util.UUID;

public record StoryReactionResponse(UUID storyId, String reaction, long reactionCount) {
}
