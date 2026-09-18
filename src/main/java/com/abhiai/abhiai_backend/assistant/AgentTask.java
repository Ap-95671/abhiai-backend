package com.abhiai.abhiai_backend.assistant;
import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.*;
@Entity @Table(name="assistant_tasks")
public class AgentTask {
    @Id public UUID id;
    @Column(nullable=false) public UUID userId;
    @Column(nullable=false) public UUID conversationId;
    @Column(length=2000,nullable=false) public String goal;
    public String status;
    @Column(columnDefinition="text") public String state;
    public int stepsUsed;
    public int toolCalls;
    public int retries;
    public long elapsedMs;
    public UUID lease;
    public Instant leaseUntil;
    public Instant createdAt;
    public Instant updatedAt;
}
