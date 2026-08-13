package com.abhiai.abhiai_backend.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class JwtServiceTest {

    @Test
    void generatedTokenCanBeParsedIntoTheOriginalPrincipal() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("test-only-jwt-secret-that-is-long-enough-for-hs256");
        properties.setAccessTokenTtl(Duration.ofMinutes(15));
        JwtService jwtService = new JwtService(properties);
        UUID userId = UUID.randomUUID();

        String token = jwtService.generateAccessToken(userId, "user@example.com");
        JwtPrincipal principal = jwtService.parseAccessToken(token);

        assertEquals(userId, principal.userId());
        assertEquals("user@example.com", principal.email());
    }
}
