package com.abhiai.abhiai_backend.entity;

import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import jakarta.persistence.*;

@Entity
@Table(name = "articles", indexes = {
        @Index(name = "idx_articles_published", columnList = "published_at,id"),
        @Index(name = "idx_articles_author_published", columnList = "author_id,published_at,id")})
public class Article {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "author_id", nullable = false) private User author;
    @Column(nullable = false, length = 180) private String title;
    @Column(nullable = false, length = 320) private String summary;
    @Column(name = "cover_image_url", length = 2048) private String coverImageUrl;
    @Column(nullable = false, columnDefinition = "TEXT") private String content;
    @Column(name = "like_count", nullable = false) private long likeCount;
    @Column(name = "comment_count", nullable = false) private long commentCount;
    @Column(name = "share_count", nullable = false) private long shareCount;
    @CreationTimestamp @Column(name = "published_at", nullable = false, updatable = false) private Instant publishedAt;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Column(name = "deleted_at") private Instant deletedAt;

    protected Article() {}
    public Article(User author, String title, String summary, String coverImageUrl, String content) {
        this.author = author; this.title = title; this.summary = summary; this.coverImageUrl = coverImageUrl; this.content = content;
    }
    public void update(String title, String summary, String coverImageUrl, String content) {
        this.title=title; this.summary=summary; this.coverImageUrl=coverImageUrl; this.content=content;
    }
    public void softDelete(){deletedAt=Instant.now();}
    public UUID getId(){return id;} public User getAuthor(){return author;} public String getTitle(){return title;}
    public String getSummary(){return summary;} public String getCoverImageUrl(){return coverImageUrl;} public String getContent(){return content;}
    public long getLikeCount(){return likeCount;} public long getCommentCount(){return commentCount;} public long getShareCount(){return shareCount;}
    public Instant getPublishedAt(){return publishedAt;} public Instant getUpdatedAt(){return updatedAt;} public Instant getDeletedAt(){return deletedAt;}
}
