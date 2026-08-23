package com.abhiai.abhiai_backend.dto.like;

import java.util.UUID;

public record PostLikeStatusResponse(UUID postId, boolean liked) {
}
