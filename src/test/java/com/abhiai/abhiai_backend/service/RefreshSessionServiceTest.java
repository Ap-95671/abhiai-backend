package com.abhiai.abhiai_backend.service;

import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import com.abhiai.abhiai_backend.dto.auth.AuthTokenResponse;
import com.abhiai.abhiai_backend.entity.*;
import com.abhiai.abhiai_backend.exception.InvalidCredentialsException;
import com.abhiai.abhiai_backend.repository.*;
import com.abhiai.abhiai_backend.security.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class RefreshSessionServiceTest {
    final RefreshSessionRepository sessions = mock(RefreshSessionRepository.class);
    final UserRepository users = mock(UserRepository.class);
    final JwtService jwt = mock(JwtService.class);
    final JwtProperties properties = new JwtProperties();
    final RefreshSessionService service = new RefreshSessionService(sessions, users, jwt, properties);
    final UUID userId = UUID.randomUUID();

    @Test void loginStoresOnlyHashAndUsesBoundedAbsoluteExpiry() {
        when(jwt.parseAccessToken("access")).thenReturn(new JwtPrincipal(userId, "test@example.com"));
        Instant before = Instant.now();
        var response = service.create(new AuthTokenResponse("access", "Bearer", 900), true);
        var saved = ArgumentCaptor.forClass(RefreshSession.class);
        verify(sessions).save(saved.capture());
        assertThat(response.refreshToken()).matches("[A-Za-z0-9_-]{43}");
        assertThat(ReflectionTestUtils.getField(saved.getValue(), "tokenHash"))
                .isEqualTo(RefreshSessionService.hash(response.refreshToken())).isNotEqualTo(response.refreshToken());
        assertThat(saved.getValue().getUserId()).isEqualTo(userId);
        assertThat(saved.getValue().getExpiresAt()).isBetween(before.plus(Duration.ofDays(30)), Instant.now().plus(Duration.ofDays(30)));
        assertThat(response.refreshExpiresInSeconds()).isEqualTo(2592000);
        assertThat(service.create(new AuthTokenResponse("access", "Bearer", 900), false).refreshExpiresInSeconds()).isEqualTo(86400);
    }
    @Test void validSessionRenewsAccessWithoutRotatingOrExtendingRefreshSession() {
        var session = new RefreshSession("hash", userId, Instant.now().plusSeconds(100));
        when(sessions.findById(RefreshSessionService.hash("token"))).thenReturn(Optional.of(session));
        var user = new User("tester", "Test", "test@example.com", "hash");
        ReflectionTestUtils.setField(user, "id", userId);
        when(users.findById(userId)).thenReturn(Optional.of(user));
        when(jwt.generateAccessToken(userId, "test@example.com")).thenReturn("renewed");
        assertThat(service.refresh("token").accessToken()).isEqualTo("renewed");
        assertThat(service.refresh("token").expiresInSeconds()).isEqualTo(900);
        verify(sessions, never()).save(any());
    }
    @Test void expiredMissingRevokedOrDeletedUserCannotRefresh() {
        when(sessions.findById(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.refresh("missing")).isInstanceOf(InvalidCredentialsException.class);
        when(sessions.findById(any())).thenReturn(Optional.of(new RefreshSession("hash", userId, Instant.now().minusSeconds(1))));
        assertThatThrownBy(() -> service.refresh("expired")).isInstanceOf(InvalidCredentialsException.class);
        verifyNoInteractions(jwt, users);
        when(sessions.findById(any())).thenReturn(Optional.of(new RefreshSession("hash", userId, Instant.now().plusSeconds(100))));
        when(users.findById(userId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.refresh("deleted-user")).isInstanceOf(InvalidCredentialsException.class);
        verifyNoInteractions(jwt);
    }
    @Test void logoutRevokesOnlyPresentedSessionByItsHash() {
        service.revoke("token");
        verify(sessions).deleteById(RefreshSessionService.hash("token"));
        verifyNoInteractions(users, jwt);
    }
}
