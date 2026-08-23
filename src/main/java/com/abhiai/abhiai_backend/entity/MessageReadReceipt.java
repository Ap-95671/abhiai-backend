package com.abhiai.abhiai_backend.entity;

import java.time.Instant;
import java.util.UUID;

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
        name = "direct_message_read_receipts",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_direct_message_receipt_message_user",
                columnNames = {"message_id", "user_id"}))
public class MessageReadReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "message_id", nullable = false)
    private DirectMessage message;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "read_at", nullable = false, updatable = false)
    private Instant readAt;

    protected MessageReadReceipt() {
    }

    public MessageReadReceipt(DirectMessage message, User user, Instant readAt) {
        this.message = message;
        this.user = user;
        this.readAt = readAt;
    }

    public UUID getId() { return id; }
    public DirectMessage getMessage() { return message; }
    public User getUser() { return user; }
    public Instant getReadAt() { return readAt; }
}
