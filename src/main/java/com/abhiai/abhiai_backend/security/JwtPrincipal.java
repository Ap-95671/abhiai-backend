package com.abhiai.abhiai_backend.security;

import java.util.UUID;

public record JwtPrincipal(UUID userId, String email) {
}
