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
@Table(name = "post_mentions", uniqueConstraints = @UniqueConstraint(
        name = "uq_post_mentions_post_user", columnNames = {"post_id", "mentioned_user_id"}))
public class PostMention {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mentioned_user_id", nullable = false)
    private User mentionedUser;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PostMention() {
    }

    public PostMention(Post post, User mentionedUser) {
        this.post = post;
        this.mentionedUser = mentionedUser;
    }

    public UUID getId() { return id; }
    public Post getPost() { return post; }
    public User getMentionedUser() { return mentionedUser; }
}
