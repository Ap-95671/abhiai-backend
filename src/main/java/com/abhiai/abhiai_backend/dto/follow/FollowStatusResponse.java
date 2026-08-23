package com.abhiai.abhiai_backend.dto.follow;

import java.util.UUID;

public record FollowStatusResponse(UUID userId, boolean following) {
}
