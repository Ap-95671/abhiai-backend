package com.abhiai.abhiai_backend.entity;

import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import jakarta.persistence.*;

@Entity
@Table(name="article_likes", uniqueConstraints=@UniqueConstraint(name="uq_article_like", columnNames={"article_id","user_id"}))
public class ArticleLike {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="article_id",nullable=false) private Article article;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="user_id",nullable=false) private User user;
    @CreationTimestamp @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
    protected ArticleLike(){} public ArticleLike(Article article,User user){this.article=article;this.user=user;}
    public UUID getId(){return id;} public Article getArticle(){return article;} public User getUser(){return user;} public Instant getCreatedAt(){return createdAt;}
}
