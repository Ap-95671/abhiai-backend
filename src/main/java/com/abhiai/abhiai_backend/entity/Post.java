package com.abhiai.abhiai_backend.entity;

import java.time.Instant;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Table;

@Entity
@Table(
        name = "posts",
        indexes = {
                @Index(name = "idx_posts_author_created", columnList = "author_id,created_at,id"),
                @Index(name = "idx_posts_visibility_created", columnList = "visibility,created_at,id"),
                @Index(name = "idx_posts_community_created", columnList = "community_id,created_at,id")
        })
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id")
    private Community community;

    @OneToMany(mappedBy = "post", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @OrderBy("position ASC")
    private List<MediaAsset> media = new ArrayList<>();

    @OneToOne(mappedBy = "post", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private PostPoll poll;

    @Column(name = "text_content", nullable = false, columnDefinition = "TEXT")
    private String textContent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PostVisibility visibility;

    @Column(name = "reply_count", nullable = false)
    private long replyCount;

    @Column(name = "like_count", nullable = false)
    private long likeCount;

    @Column(name = "repost_count", nullable = false)
    private long repostCount;

    @Column(name = "bookmark_count", nullable = false)
    private long bookmarkCount;

    @Column(name = "view_count", nullable = false)
    private long viewCount;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "pinned_at")
    private Instant pinnedAt;

    protected Post() {
    }

    public Post(User author, String textContent, PostVisibility visibility) {
        this(author, textContent, visibility, null);
    }

    public Post(
            User author,
            String textContent,
            PostVisibility visibility,
            Community community) {
        this.author = author;
        this.textContent = textContent;
        this.visibility = visibility;
        this.community = community;
    }

    public void update(String textContent, PostVisibility visibility) {
        if (textContent != null) {
            this.textContent = textContent;
        }
        if (visibility != null) {
            this.visibility = visibility;
        }
    }

    public void softDelete() {
        this.deletedAt = Instant.now();
        this.pinnedAt = null;
    }
    public void restore(){this.deletedAt=null;}

    public void pin() { this.pinnedAt = Instant.now(); }

    public void unpin() { this.pinnedAt = null; }
    public void attachMedia(List<MediaAsset> assets) { for (short i=0;i<assets.size();i++){assets.get(i).attachTo(this,i);media.add(assets.get(i));} }
    public List<MediaAsset> getMedia() { return List.copyOf(media); }
    public void attachPoll(PostPoll poll) { this.poll = poll; }
    public PostPoll getPoll() { return poll; }

    public UUID getId() {
        return id;
    }

    public User getAuthor() {
        return author;
    }

    public Community getCommunity() {
        return community;
    }

    public String getTextContent() {
        return textContent;
    }

    public PostVisibility getVisibility() {
        return visibility;
    }

    public long getReplyCount() {
        return replyCount;
    }

    public long getLikeCount() {
        return likeCount;
    }

    public long getRepostCount() {
        return repostCount;
    }

    public long getBookmarkCount() {
        return bookmarkCount;
    }

    public long getViewCount() {
        return viewCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public Instant getPinnedAt() { return pinnedAt; }
}
