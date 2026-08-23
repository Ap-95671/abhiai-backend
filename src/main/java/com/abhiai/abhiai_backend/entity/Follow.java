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
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "user_follows",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_follows_follower_following",
                        columnNames = {"follower_id", "following_id"})
        },
        indexes = {
                @Index(
                        name = "idx_user_follows_follower_created",
                        columnList = "follower_id,created_at,id"),
                @Index(
                        name = "idx_user_follows_following_created",
                        columnList = "following_id,created_at,id")
        })
public class Follow {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "follower_id", nullable = false)
    private User follower;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "following_id", nullable = false)
    private User following;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Follow() {
    }

    public Follow(User follower, User following) {
        this.follower = follower;
        this.following = following;
    }

    public UUID getId() {
        return id;
    }

    public User getFollower() {
        return follower;
    }

    public User getFollowing() {
        return following;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
