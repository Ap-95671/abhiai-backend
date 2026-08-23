package com.abhiai.abhiai_backend.dto.bookmark;

import java.util.UUID;

public record PostBookmarkStatusResponse(UUID postId, boolean bookmarked) {
}
