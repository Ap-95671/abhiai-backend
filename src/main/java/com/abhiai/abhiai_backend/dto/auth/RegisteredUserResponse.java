package com.abhiai.abhiai_backend.dto.auth;

import java.time.Instant;
import java.util.UUID;

import com.abhiai.abhiai_backend.entity.User;

public record RegisteredUserResponse(
        UUID id,
        String username,
        String displayName,
        String email,
        Instant createdAt) {

    public static RegisteredUserResponse from(User user) {
        return new RegisteredUserResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getEmail(),
                user.getCreatedAt());
    }
}
