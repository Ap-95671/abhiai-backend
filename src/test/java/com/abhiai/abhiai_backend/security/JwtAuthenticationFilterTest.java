package com.abhiai.abhiai_backend.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtAuthenticationFilterTest {
    private final UUID authenticatedUser = UUID.randomUUID();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validBearerTokenCreatesAuthenticatedPrincipal() throws Exception {
        JwtService jwtService = jwtService();
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService);
        UUID userId = authenticatedUser;
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + jwtService.generateAccessToken(userId, "user@example.com", "session"));

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertTrue(authentication.isAuthenticated());
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        assertEquals(userId, principal.userId());
        assertEquals("user@example.com", principal.email());
    }

    @Test
    void invalidBearerTokenDoesNotAuthenticateRequest() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer not-a-valid-token");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    private JwtService jwtService() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("test-only-jwt-secret-that-is-long-enough-for-hs256");
        properties.setAccessTokenTtl(Duration.ofMinutes(15));
        var sessions = org.mockito.Mockito.mock(com.abhiai.abhiai_backend.repository.RefreshSessionRepository.class);
        org.mockito.Mockito.when(sessions.findById("session")).thenAnswer(invocation -> java.util.Optional.of(
                new com.abhiai.abhiai_backend.entity.RefreshSession("session", authenticatedUser, java.time.Instant.now().plusSeconds(900))));
        return new JwtService(properties, sessions);
    }
}
