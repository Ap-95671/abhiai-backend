package com.abhiai.abhiai_backend.assistant;
import java.util.UUID;
import java.time.Instant;
import jakarta.persistence.*;
@Entity @Table(name="assistant_pending_actions")
public class PendingAssistantAction {
    @Id public UUID id;
    public UUID userId;
    public UUID taskId;
    public String actionType;
    @Column(columnDefinition="text") public String exactPayload;
    public String payloadHash;
    public String resourceHash;
    public String status;
    public Instant createdAt;
    public Instant expiresAt;
}
