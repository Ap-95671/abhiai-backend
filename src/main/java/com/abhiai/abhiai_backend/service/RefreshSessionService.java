package com.abhiai.abhiai_backend.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.abhiai.abhiai_backend.dto.auth.*;
import com.abhiai.abhiai_backend.entity.RefreshSession;
import com.abhiai.abhiai_backend.exception.InvalidCredentialsException;
import com.abhiai.abhiai_backend.repository.*;
import com.abhiai.abhiai_backend.security.*;

@Service
public class RefreshSessionService {
    private final RefreshSessionRepository sessions;
    private final UserRepository users;
    private final JwtService jwt;
    private final JwtProperties properties;
    private final SecureRandom random = new SecureRandom();

    public RefreshSessionService(RefreshSessionRepository sessions, UserRepository users,
                                 JwtService jwt, JwtProperties properties) {
        this.sessions = sessions; this.users = users; this.jwt = jwt; this.properties = properties;
    }

    @Transactional
    public SessionTokenResponse create(AuthTokenResponse access, boolean rememberMe) {
        byte[] bytes = new byte[32]; random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Duration ttl = rememberMe ? properties.getRefreshTokenTtl() : properties.getSessionTokenTtl();
        sessions.save(new RefreshSession(hash(token), jwt.parseAccessToken(access.accessToken()).userId(), Instant.now().plus(ttl)));
        return new SessionTokenResponse(access.accessToken(), access.tokenType(), access.expiresInSeconds(), token, ttl.toSeconds());
    }

    @Transactional(readOnly = true)
    public AuthTokenResponse refresh(String token) {
        var session = sessions.findById(hash(token)).filter(s -> s.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(InvalidCredentialsException::new);
        var user = users.findById(session.getUserId()).orElseThrow(InvalidCredentialsException::new);
        // Absolute expiry is never extended; concurrent refreshes can reuse this revocable session.
        return new AuthTokenResponse(jwt.generateAccessToken(user.getId(), user.getEmail()), "Bearer",
                properties.getAccessTokenTtl().toSeconds());
    }

    @Transactional
    public void revoke(String token) { sessions.deleteById(hash(token)); }

    static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
}
