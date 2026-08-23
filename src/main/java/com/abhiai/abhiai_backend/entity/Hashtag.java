package com.abhiai.abhiai_backend.entity;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "hashtags")
public class Hashtag {

    @Id
    private UUID id;

    @Column(name = "normalized_tag", nullable = false, unique = true, length = 50)
    private String normalizedTag;

    @Column(name = "display_tag", nullable = false, length = 50)
    private String displayTag;

    @Column(name = "post_count", nullable = false)
    private long postCount;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Hashtag() {
    }

    public Hashtag(UUID id, String normalizedTag, String displayTag) {
        this.id = id;
        this.normalizedTag = normalizedTag;
        this.displayTag = displayTag;
    }

    public UUID getId() { return id; }
    public String getNormalizedTag() { return normalizedTag; }
    public String getDisplayTag() { return displayTag; }
    public long getPostCount() { return postCount; }
    public Instant getCreatedAt() { return createdAt; }
}
