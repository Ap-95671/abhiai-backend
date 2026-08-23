package com.abhiai.abhiai_backend.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "direct_conversations")
public class DirectConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "participant_key", nullable = false, unique = true, length = 73)
    private String participantKey;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ConversationParticipant> participants = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected DirectConversation() {
    }

    public DirectConversation(String participantKey) {
        this.participantKey = participantKey;
    }

    public void addParticipant(User user) {
        participants.add(new ConversationParticipant(this, user));
    }

    public void touch() {
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getParticipantKey() { return participantKey; }
    public List<ConversationParticipant> getParticipants() { return List.copyOf(participants); }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
