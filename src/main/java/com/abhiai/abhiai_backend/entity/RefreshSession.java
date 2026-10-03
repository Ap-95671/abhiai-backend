package com.abhiai.abhiai_backend.entity;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.*;

@Entity
@Table(name = "refresh_sessions")
public class RefreshSession {
    @Id @Column(length = 64) private String tokenHash;
    @Column(nullable = false) private UUID userId;
    @Column(nullable = false) private Instant expiresAt;
    protected RefreshSession() { }
    public RefreshSession(String tokenHash, UUID userId, Instant expiresAt) {
        this.tokenHash = tokenHash; this.userId = userId; this.expiresAt = expiresAt;
    }
    public UUID getUserId() { return userId; }
    public Instant getExpiresAt() { return expiresAt; }
}
