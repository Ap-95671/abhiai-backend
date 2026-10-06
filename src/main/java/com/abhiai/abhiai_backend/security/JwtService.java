package com.abhiai.abhiai_backend.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import com.abhiai.abhiai_backend.repository.RefreshSessionRepository;
import io.jsonwebtoken.JwtException;

@Service
public class JwtService {

    private final JwtProperties jwtProperties;
    private final SecretKey signingKey;
    private final RefreshSessionRepository sessions;

    public JwtService(JwtProperties jwtProperties, RefreshSessionRepository sessions) {
        this.jwtProperties = jwtProperties;
        this.sessions = sessions;
        if (jwtProperties.getAccessTokenTtl().isZero() || jwtProperties.getAccessTokenTtl().isNegative()) {
            throw new IllegalStateException("JWT access-token TTL must be positive");
        }
        this.signingKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(UUID userId, String email) {
        return generateAccessToken(userId, email, null);
    }

    public String generateAccessToken(UUID userId, String email, String sessionId) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(jwtProperties.getAccessTokenTtl());

        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("sid", sessionId)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();
    }

    public JwtPrincipal parseAccessToken(String token) {
        return principal(claims(token));
    }

    /** Authentication requires the same live session that issues refresh tokens. */
    public JwtPrincipal parseSessionAccessToken(String token) {
        Claims claims = claims(token);
        String sessionId = claims.get("sid", String.class);
        if (sessionId == null) throw new JwtException("Session renewal required");
        JwtPrincipal principal = principal(claims);
        sessions.findById(sessionId)
                .filter(session -> session.getUserId().equals(principal.userId())
                        && session.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> new JwtException("Session revoked or expired"));
        return principal;
    }

    private Claims claims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

    }

    private JwtPrincipal principal(Claims claims) {
        return new JwtPrincipal(
                UUID.fromString(claims.getSubject()),
                claims.get("email", String.class));
    }
}
