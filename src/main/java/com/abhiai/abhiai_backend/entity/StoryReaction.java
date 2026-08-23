package com.abhiai.abhiai_backend.entity;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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
@Table(name = "story_reactions", uniqueConstraints = @UniqueConstraint(
        name = "uq_story_reactions_story_user", columnNames = {"story_id", "user_id"}))
public class StoryReaction {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "story_id", nullable = false) private Story story;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false) private User user;
    @Column(nullable = false, length = 16) private String reaction;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    protected StoryReaction() {}
    public StoryReaction(Story story, User user, String reaction) { this.story=story; this.user=user; this.reaction=reaction; }
    public void update(String reaction) { this.reaction=reaction; }
    public Story getStory(){return story;} public User getUser(){return user;} public String getReaction(){return reaction;}
}
