package com.abhiai.abhiai_backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.abhiai.abhiai_backend.dto.auth.AuthTokenResponse;
import com.abhiai.abhiai_backend.dto.auth.LoginRequest;
import com.abhiai.abhiai_backend.dto.auth.RegisterUserRequest;
import com.abhiai.abhiai_backend.dto.auth.RegisteredUserResponse;
import com.abhiai.abhiai_backend.service.AuthService;
import com.abhiai.abhiai_backend.service.UserService;

import jakarta.validation.Valid;
import com.abhiai.abhiai_backend.dto.auth.SessionTokenResponse;
import com.abhiai.abhiai_backend.dto.auth.RefreshRequest;
import com.abhiai.abhiai_backend.service.RefreshSessionService;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserService userService;
    private final AuthService authService;

    private final RefreshSessionService sessions;

    public AuthController(UserService userService, AuthService authService, RefreshSessionService sessions) {
        this.sessions = sessions;
        this.userService = userService;
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisteredUserResponse> register(@Valid @RequestBody RegisterUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<SessionTokenResponse> login(@Valid @RequestBody LoginRequest request,
            @RequestParam(defaultValue = "true") boolean rememberMe) {
        return ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore())
                .body(sessions.create(authService.login(request), rememberMe));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthTokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore())
                .body(sessions.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        sessions.revoke(request.refreshToken());
        return ResponseEntity.noContent().build();
    }
}
