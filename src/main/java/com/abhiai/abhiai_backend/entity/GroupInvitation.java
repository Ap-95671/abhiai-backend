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
        name = "group_invitations",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_group_invitation_conversation_invitee",
                columnNames = {"conversation_id", "invitee_id"}))
public class GroupInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private GroupConversation conversation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inviter_id", nullable = false)
    private User inviter;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invitee_id", nullable = false)
    private User invitee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private GroupInvitationStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "responded_at")
    private Instant respondedAt;

    protected GroupInvitation() {
    }

    public GroupInvitation(GroupConversation conversation, User inviter, User invitee) {
        this.conversation = conversation;
        this.inviter = inviter;
        this.invitee = invitee;
        this.status = GroupInvitationStatus.PENDING;
    }

    public void renew(User inviter) {
        this.inviter = inviter;
        status = GroupInvitationStatus.PENDING;
        respondedAt = null;
    }

    public void respond(GroupInvitationStatus status, Instant respondedAt) {
        this.status = status;
        this.respondedAt = respondedAt;
    }

    public UUID getId() { return id; }
    public GroupConversation getConversation() { return conversation; }
    public User getInviter() { return inviter; }
    public User getInvitee() { return invitee; }
    public GroupInvitationStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getRespondedAt() { return respondedAt; }
}
