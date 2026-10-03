package com.abhiai.abhiai_backend.dto.auth;

/** Refresh credentials are consumed only by the frontend server, never browser JavaScript. */
public record SessionTokenResponse(String accessToken, String tokenType, long expiresInSeconds,
                                   String refreshToken, long refreshExpiresInSeconds) { }
