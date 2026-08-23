package com.abhiai.abhiai_backend.dto.user;

import java.time.Instant;
import java.util.UUID;

import com.abhiai.abhiai_backend.entity.UserBlock;

public record BlockedUserResponse(UUID id, String username, String displayName, String profilePicture, Instant blockedAt) {
    public static BlockedUserResponse from(UserBlock block) {
        var user = block.getBlocked();
        return new BlockedUserResponse(user.getId(), user.getUsername(), user.getDisplayName(), user.getProfilePicture(), block.getCreatedAt());
    }
}
