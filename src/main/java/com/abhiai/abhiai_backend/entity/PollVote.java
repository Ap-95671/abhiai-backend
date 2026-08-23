package com.abhiai.abhiai_backend.entity;

import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import jakarta.persistence.*;

@Entity
@Table(name = "poll_votes", uniqueConstraints = @UniqueConstraint(name = "uk_poll_votes_poll_user", columnNames = {"poll_id", "user_id"}))
public class PollVote {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "poll_id", nullable = false) private PostPoll poll;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "choice_id", nullable = false) private PollChoice choice;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false) private User user;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    protected PollVote() {}
    public PollVote(PostPoll poll, PollChoice choice, User user) { this.poll = poll; this.choice = choice; this.user = user; }
    public UUID getId() { return id; }
    public PollChoice getChoice() { return choice; }
}
