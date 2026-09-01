package com.abhiai.abhiai_backend.entity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 30)
    private String username;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 60)
    private String passwordHash;

    @Column(length = 160)
    private String bio;

    @Column(name = "profile_picture_url", length = 2048)
    private String profilePicture;

    @Column(name = "cover_picture_url", length = 2048)
    private String coverPicture;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_media_id")
    private MediaAsset profileMedia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cover_media_id")
    private MediaAsset coverMedia;

    @Column(length = 100)
    private String location;

    @Column(length = 2048)
    private String website;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "verified_status", nullable = false, length = 20)
    private VerifiedStatus verifiedStatus = VerifiedStatus.NONE;

    @Column(name = "follower_count", nullable = false)
    private long followerCount;

    @Column(name = "following_count", nullable = false)
    private long followingCount;

    @Column(name = "post_count", nullable = false)
    private long postCount;

    @Column(name = "show_likes_on_profile", nullable = false)
    private boolean showLikesOnProfile = true;
    @Enumerated(EnumType.STRING) @Column(name="account_privacy",nullable=false,length=16)
    private AccountPrivacy accountPrivacy = AccountPrivacy.PUBLIC;

    @Column(name = "ai_memory_enabled", nullable = false)
    private boolean aiMemoryEnabled;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected User() {
    }

    public User(String username, String displayName, String email, String passwordHash) {
        this.username = username;
        this.displayName = displayName;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    public UUID getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getBio() {
        return bio;
    }

    public String getProfilePicture() {
        return profilePicture;
    }

    public String getCoverPicture() {
        return coverPicture;
    }
    public MediaAsset getProfileMedia() { return profileMedia; }
    public MediaAsset getCoverMedia() { return coverMedia; }

    public String getLocation() {
        return location;
    }

    public String getWebsite() {
        return website;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public VerifiedStatus getVerifiedStatus() {
        return verifiedStatus;
    }

    public long getFollowerCount() {
        return followerCount;
    }

    public long getFollowingCount() {
        return followingCount;
    }

    public long getPostCount() {
        return postCount;
    }

    public boolean isShowLikesOnProfile() {
        return showLikesOnProfile;
    }
    public AccountPrivacy getAccountPrivacy(){return accountPrivacy;}
    public void changeAccountPrivacy(AccountPrivacy privacy){this.accountPrivacy=java.util.Objects.requireNonNull(privacy);}
    public boolean isAiMemoryEnabled() { return aiMemoryEnabled; }
    public void changeAiMemoryEnabled(boolean enabled) { this.aiMemoryEnabled = enabled; }
    public void changeVerifiedStatus(VerifiedStatus status){this.verifiedStatus=java.util.Objects.requireNonNull(status);}

    public void updateProfile(
            String username,
            String displayName,
            String bio,
            String profilePicture,
            String coverPicture,
            MediaAsset profileMedia,
            MediaAsset coverMedia,
            String location,
            String website,
            LocalDate dateOfBirth,
            Boolean showLikesOnProfile) {
        if (username != null) {
            this.username = username;
        }
        if (displayName != null) {
            this.displayName = displayName;
        }
        if (bio != null) {
            this.bio = emptyToNull(bio);
        }
        if (profilePicture != null) {
            this.profilePicture = emptyToNull(profilePicture);
        }
        if (coverPicture != null) {
            this.coverPicture = emptyToNull(coverPicture);
        }
        if (profileMedia != null) { this.profileMedia = profileMedia; this.profilePicture = null; }
        if (coverMedia != null) { this.coverMedia = coverMedia; this.coverPicture = null; }
        if (location != null) {
            this.location = emptyToNull(location);
        }
        if (website != null) {
            this.website = emptyToNull(website);
        }
        if (dateOfBirth != null) {
            this.dateOfBirth = dateOfBirth;
        }
        if (showLikesOnProfile != null) {
            this.showLikesOnProfile = showLikesOnProfile;
        }
    }

    private String emptyToNull(String value) {
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
