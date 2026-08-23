package com.abhiai.abhiai_backend.dto.story;

import java.util.UUID;

public record StoryViewResponse(UUID storyId, long viewCount, boolean counted) {
}
