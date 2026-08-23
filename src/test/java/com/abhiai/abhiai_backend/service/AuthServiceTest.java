package com.abhiai.abhiai_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.abhiai.abhiai_backend.dto.auth.AuthTokenResponse;
import com.abhiai.abhiai_backend.dto.auth.LoginRequest;
import com.abhiai.abhiai_backend.entity.User;
import com.abhiai.abhiai_backend.exception.InvalidCredentialsException;
import com.abhiai.abhiai_backend.repository.UserRepository;
import com.abhiai.abhiai_backend.security.JwtProperties;
import com.abhiai.abhiai_backend.security.JwtService;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setAccessTokenTtl(Duration.ofMinutes(15));
        authService = new AuthService(userRepository, passwordEncoder, jwtService, jwtProperties);
    }

    @Test
    void loginReturnsBearerTokenForValidCredentials() {
        User user = new User("abhishek", "Abhishek", "user@example.com", "bcrypt-hash");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("safe-password", "bcrypt-hash")).thenReturn(true);
        when(jwtService.generateAccessToken(any(), eq("user@example.com"))).thenReturn("signed-token");

        AuthTokenResponse response = authService.login(new LoginRequest(" USER@example.com ", "safe-password"));

        assertEquals("signed-token", response.accessToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(900, response.expiresInSeconds());
        verify(passwordEncoder).matches("safe-password", "bcrypt-hash");
    }

    @Test
    void loginUsesGenericErrorWhenEmailDoesNotExist() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class,
                () -> authService.login(new LoginRequest("missing@example.com", "safe-password")));

        verifyNoInteractions(passwordEncoder, jwtService);
    }
}
