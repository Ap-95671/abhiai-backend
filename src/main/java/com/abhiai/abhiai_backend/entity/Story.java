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

@Entity
@Table(name = "stories")
public class Story {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "media_id", unique = true)
    private MediaAsset media;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private StoryType type;

    @Column(name = "text_content", length = 500)
    private String textContent;

    @Column(name = "background_color", nullable = false, length = 7)
    private String backgroundColor;

    @Column(name = "view_count", nullable = false)
    private long viewCount;

    @Column(name = "reaction_count", nullable = false)
    private long reactionCount;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Story() {
    }

    public Story(
            User author,
            MediaAsset media,
            StoryType type,
            String textContent,
            String backgroundColor,
            Instant expiresAt) {
        this.author = author;
        this.media = media;
        this.type = type;
        this.textContent = textContent;
        this.backgroundColor = backgroundColor;
        this.expiresAt = expiresAt;
    }

    public UUID getId() { return id; }
    public User getAuthor() { return author; }
    public MediaAsset getMedia() { return media; }
    public StoryType getType() { return type; }
    public String getTextContent() { return textContent; }
    public String getBackgroundColor() { return backgroundColor; }
    public long getViewCount() { return viewCount; }
    public long getReactionCount() { return reactionCount; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
}
