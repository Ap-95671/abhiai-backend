package com.abhiai.abhiai_backend.entity;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "direct_conversation_participants",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_direct_participant_conversation_user",
                columnNames = {"conversation_id", "user_id"}))
public class ConversationParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private DirectConversation conversation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @CreationTimestamp
    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    protected ConversationParticipant() {
    }

    public ConversationParticipant(DirectConversation conversation, User user) {
        this.conversation = conversation;
        this.user = user;
    }

    public UUID getId() { return id; }
    public DirectConversation getConversation() { return conversation; }
    public User getUser() { return user; }
    public Instant getJoinedAt() { return joinedAt; }
}
