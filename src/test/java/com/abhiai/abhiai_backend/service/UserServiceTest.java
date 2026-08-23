package com.abhiai.abhiai_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.abhiai.abhiai_backend.dto.auth.RegisterUserRequest;
import com.abhiai.abhiai_backend.dto.auth.RegisteredUserResponse;
import com.abhiai.abhiai_backend.entity.User;
import com.abhiai.abhiai_backend.exception.EmailAlreadyRegisteredException;
import com.abhiai.abhiai_backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UsernamePolicy usernamePolicy;

    @InjectMocks
    private UserService userService;

    @Test
    void registerPersistsNormalizedEmailAndPasswordHash() {
        RegisterUserRequest request = new RegisterUserRequest("  Abhishek  ", "  USER@Example.COM ", "safe-password");
        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(usernamePolicy.generateInitialUsername(eq("user@example.com"), any())).thenReturn("user");
        when(passwordEncoder.encode("safe-password")).thenReturn("bcrypt-hash");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegisteredUserResponse response = userService.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(userCaptor.capture());
        assertEquals("Abhishek", userCaptor.getValue().getDisplayName());
        assertEquals("user", userCaptor.getValue().getUsername());
        assertEquals("user@example.com", userCaptor.getValue().getEmail());
        assertEquals("bcrypt-hash", userCaptor.getValue().getPasswordHash());
        assertEquals("user@example.com", response.email());
        assertEquals("user", response.username());
    }

    @Test
    void registerRejectsAnExistingEmailWithoutHashingThePassword() {
        RegisterUserRequest request = new RegisterUserRequest("Abhishek", "user@example.com", "safe-password");
        when(userRepository.existsByEmail("user@example.com")).thenReturn(true);

        assertThrows(EmailAlreadyRegisteredException.class, () -> userService.register(request));

        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).saveAndFlush(any());
    }
}
