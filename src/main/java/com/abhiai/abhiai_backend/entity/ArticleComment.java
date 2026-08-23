package com.abhiai.abhiai_backend.entity;

import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import jakarta.persistence.*;

@Entity
@Table(name="article_comments",indexes=@Index(name="idx_article_comments_article_created",columnList="article_id,created_at,id"))
public class ArticleComment {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="article_id",nullable=false) private Article article;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="author_id",nullable=false) private User author;
    @Column(nullable=false,length=2000) private String content;
    @CreationTimestamp @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
    @UpdateTimestamp @Column(name="updated_at",nullable=false) private Instant updatedAt;
    @Column(name="deleted_at") private Instant deletedAt;
    protected ArticleComment(){} public ArticleComment(Article article,User author,String content){this.article=article;this.author=author;this.content=content;}
    public void softDelete(){deletedAt=Instant.now();}
    public UUID getId(){return id;} public Article getArticle(){return article;} public User getAuthor(){return author;} public String getContent(){return content;}
    public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;} public Instant getDeletedAt(){return deletedAt;}
}
