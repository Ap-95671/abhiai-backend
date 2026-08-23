package com.abhiai.abhiai_backend.entity;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
        name = "group_participants",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_group_participant_conversation_user",
                columnNames = {"conversation_id", "user_id"}))
public class GroupParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private GroupConversation conversation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private GroupRole role;

    @CreationTimestamp
    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    protected GroupParticipant() {
    }

    public GroupParticipant(GroupConversation conversation, User user, GroupRole role) {
        this.conversation = conversation;
        this.user = user;
        this.role = role;
    }

    public void changeRole(GroupRole role) { this.role = role; }

    public UUID getId() { return id; }
    public GroupConversation getConversation() { return conversation; }
    public User getUser() { return user; }
    public GroupRole getRole() { return role; }
    public Instant getJoinedAt() { return joinedAt; }
}
