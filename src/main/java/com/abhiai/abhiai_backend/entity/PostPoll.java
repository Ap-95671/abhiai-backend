package com.abhiai.abhiai_backend.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

@Entity
@Table(name = "post_polls")
public class PostPoll {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "post_id", nullable = false, unique = true) private Post post;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(name = "total_votes", nullable = false) private long totalVotes;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @OneToMany(mappedBy = "poll", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC") private List<PollChoice> choices = new ArrayList<>();

    protected PostPoll() {}
    public PostPoll(Post post, Instant expiresAt, List<String> choiceTexts) {
        this.post = post; this.expiresAt = expiresAt;
        for (short position = 0; position < choiceTexts.size(); position++) {
            choices.add(new PollChoice(this, choiceTexts.get(position), position));
        }
    }
    public void recordVote(PollChoice choice) { choice.recordVote(); totalVotes++; }
    public boolean isExpired() { return !Instant.now().isBefore(expiresAt); }
    public UUID getId() { return id; }
    public Post getPost() { return post; }
    public Instant getExpiresAt() { return expiresAt; }
    public long getTotalVotes() { return totalVotes; }
    public Instant getCreatedAt() { return createdAt; }
    public List<PollChoice> getChoices() { return List.copyOf(choices); }
}
