package com.abhiai.abhiai_backend.entity;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
        name = "communities",
        uniqueConstraints = @UniqueConstraint(name = "uk_communities_slug", columnNames = "slug"),
        indexes = @Index(
                name = "idx_communities_discovery",
                columnList = "privacy,member_count,created_at,id"))
public class Community {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 64)
    private String slug;

    @Column(length = 1000)
    private String description;

    @Column(name = "icon_url", length = 2048)
    private String iconUrl;

    @Column(name = "banner_url", length = 2048)
    private String bannerUrl;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "member_count", nullable = false)
    private long memberCount = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CommunityPrivacy privacy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Community() {
    }

    public Community(
            String name,
            String slug,
            String description,
            String iconUrl,
            String bannerUrl,
            User owner,
            CommunityPrivacy privacy) {
        this.name = name;
        this.slug = slug;
        this.description = description;
        this.iconUrl = iconUrl;
        this.bannerUrl = bannerUrl;
        this.owner = owner;
        this.privacy = privacy;
    }

    public void addMember() {
        memberCount++;
    }

    public void removeMember() {
        if (memberCount > 1) {
            memberCount--;
        }
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSlug() {
        return slug;
    }

    public String getDescription() {
        return description;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public String getBannerUrl() {
        return bannerUrl;
    }

    public User getOwner() {
        return owner;
    }

    public long getMemberCount() {
        return memberCount;
    }

    public CommunityPrivacy getPrivacy() {
        return privacy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
