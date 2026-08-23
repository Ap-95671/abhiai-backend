package com.abhiai.abhiai_backend.entity;

import java.util.UUID;
import jakarta.persistence.*;

@Entity
@Table(name = "poll_choices", uniqueConstraints = {
        @UniqueConstraint(name = "uk_poll_choices_poll_position", columnNames = {"poll_id", "position"}),
        @UniqueConstraint(name = "uk_poll_choices_id_poll", columnNames = {"id", "poll_id"})})
public class PollChoice {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "poll_id", nullable = false) private PostPoll poll;
    @Column(name = "choice_text", nullable = false, length = 100) private String text;
    @Column(nullable = false) private short position;
    @Column(name = "vote_count", nullable = false) private long voteCount;
    protected PollChoice() {}
    PollChoice(PostPoll poll, String text, short position) { this.poll = poll; this.text = text; this.position = position; }
    void recordVote() { voteCount++; }
    public UUID getId() { return id; }
    public PostPoll getPoll() { return poll; }
    public String getText() { return text; }
    public short getPosition() { return position; }
    public long getVoteCount() { return voteCount; }
}
