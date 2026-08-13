package com.abhiai.abhiai_backend.dto.auth;

public record AuthTokenResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds) {
}
