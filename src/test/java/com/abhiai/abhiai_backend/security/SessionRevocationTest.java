package com.abhiai.abhiai_backend.security;

import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import com.abhiai.abhiai_backend.entity.RefreshSession;
import com.abhiai.abhiai_backend.repository.RefreshSessionRepository;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SessionRevocationTest {
    final RefreshSessionRepository sessions = mock(RefreshSessionRepository.class);
    final JwtService jwt = jwt();
    private JwtService jwt() {
        var properties = new JwtProperties();
        properties.setSecret("test-only-session-revocation-secret-long-enough");
        return new JwtService(properties, sessions);
    }
    final UUID user = UUID.randomUUID();

    @Test void everyAccessTokenFromTheRevokedSessionIsRejectedAndAnotherSessionSurvives() {
        when(sessions.findById("first")).thenReturn(Optional.of(new RefreshSession("first", user, Instant.now().plusSeconds(900))));
        when(sessions.findById("second")).thenReturn(Optional.of(new RefreshSession("second", user, Instant.now().plusSeconds(900))));
        var original = jwt.generateAccessToken(user, "qa@example.test", "first");
        var renewed = jwt.generateAccessToken(user, "qa@example.test", "first");
        var other = jwt.generateAccessToken(user, "qa@example.test", "second");
        assertThat(jwt.parseSessionAccessToken(original).userId()).isEqualTo(user);
        when(sessions.findById("first")).thenReturn(Optional.empty());
        for (String token : List.of(original, renewed))
            assertThatThrownBy(() -> jwt.parseSessionAccessToken(token)).isInstanceOf(io.jsonwebtoken.JwtException.class);
        assertThat(jwt.parseSessionAccessToken(other).userId()).isEqualTo(user);
    }

    @Test void expiredSessionsWrongOwnersAndUnboundLegacyTokensCannotAuthenticate() {
        var token = jwt.generateAccessToken(user, "qa@example.test", "session");
        when(sessions.findById("session")).thenReturn(Optional.of(new RefreshSession("session", user, Instant.now().minusSeconds(1))));
        assertThatThrownBy(() -> jwt.parseSessionAccessToken(token)).isInstanceOf(io.jsonwebtoken.JwtException.class);
        when(sessions.findById("session")).thenReturn(Optional.of(new RefreshSession("session", UUID.randomUUID(), Instant.now().plusSeconds(900))));
        assertThatThrownBy(() -> jwt.parseSessionAccessToken(token)).isInstanceOf(io.jsonwebtoken.JwtException.class);
        assertThatThrownBy(() -> jwt.parseSessionAccessToken(jwt.generateAccessToken(user, "qa@example.test")))
                .isInstanceOf(io.jsonwebtoken.JwtException.class);
    }
}
