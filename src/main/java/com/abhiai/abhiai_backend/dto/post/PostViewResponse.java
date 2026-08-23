package com.abhiai.abhiai_backend.dto.post;

import java.util.UUID;

public record PostViewResponse(UUID postId, long viewCount, boolean counted) {
}
