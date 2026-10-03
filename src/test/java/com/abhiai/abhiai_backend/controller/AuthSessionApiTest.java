package com.abhiai.abhiai_backend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.abhiai.abhiai_backend.dto.auth.*;
import com.abhiai.abhiai_backend.service.*;
import com.abhiai.abhiai_backend.exception.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthSessionApiTest {
    @Test void refreshAndLogoutContractAndInvalidSessionStatus() throws Exception {
        var sessions = mock(RefreshSessionService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new AuthController(mock(UserService.class), mock(AuthService.class), sessions))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        String token = "a".repeat(43), body = "{\"refreshToken\":\"" + token + "\"}";
        when(sessions.refresh(token)).thenReturn(new AuthTokenResponse("renewed", "Bearer", 900));
        mvc.perform(post("/api/v1/auth/refresh").contentType("application/json").content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.accessToken").value("renewed"))
                .andExpect(jsonPath("$.refreshToken").doesNotExist()).andExpect(header().string("Cache-Control", "no-store"));
        mvc.perform(post("/api/v1/auth/logout").contentType("application/json").content(body)).andExpect(status().isNoContent());
        verify(sessions).revoke(token);
        when(sessions.refresh(token)).thenThrow(new InvalidCredentialsException());
        mvc.perform(post("/api/v1/auth/refresh").contentType("application/json").content(body)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/refresh").contentType("application/json").content("{\"refreshToken\":\"short\"}"))
                .andExpect(status().isBadRequest());
    }
    @Test void loginKeepsAccessContractAndAddsBoundedRefreshCredentialForServerBridge() throws Exception {
        var sessions = mock(RefreshSessionService.class);
        var auth = mock(AuthService.class);
        var access = new AuthTokenResponse("access", "Bearer", 900);
        when(auth.login(any())).thenReturn(access);
        when(sessions.create(access, false)).thenReturn(new SessionTokenResponse("access", "Bearer", 900, "opaque", 86400));
        var mvc = MockMvcBuilders.standaloneSetup(new AuthController(mock(UserService.class), auth, sessions)).build();
        mvc.perform(post("/api/v1/auth/login?rememberMe=false").contentType("application/json")
                .content("{\"email\":\"user@example.com\",\"password\":\"password\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.expiresInSeconds").value(900))
                .andExpect(jsonPath("$.refreshExpiresInSeconds").value(86400))
                .andExpect(header().string("Cache-Control", "no-store"));
    }
}
