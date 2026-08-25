package com.abhiai.abhiai_backend.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.OneToMany;

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
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import com.abhiai.abhiai_backend.ai.orchestration.SelectionMode;

@Entity
@Table(name = "conversations", indexes = {
        @Index(name = "idx_conversations_user_updated_at", columnList = "user_id,updated_at")
})
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(
        mappedBy = "conversation",
        cascade = CascadeType.ALL,
        orphanRemoval = true)
private List<Message> messages = new ArrayList<>();

    @Column(nullable = false, length = 255)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "model_selection_mode", nullable = false, length = 16)
    private SelectionMode modelSelectionMode = SelectionMode.AUTO;

    @Column(name = "preferred_model_id", length = 160)
    private String preferredModelId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Conversation() {
    }

    public Conversation(User user, String title) {
        this.user = user;
        this.title = title;
    }

    public void touch() {
        this.updatedAt = Instant.now();
    }
    public void rename(String title) {
    this.title = title;
    this.updatedAt = Instant.now();
    }

    public void selectModel(SelectionMode mode, String modelId) {
        this.modelSelectionMode = mode == null ? SelectionMode.AUTO : mode;
        this.preferredModelId = this.modelSelectionMode == SelectionMode.AUTO ? null : modelId;
        touch();
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
    public SelectionMode getModelSelectionMode() { return modelSelectionMode; }
    public String getPreferredModelId() { return preferredModelId; }
}
