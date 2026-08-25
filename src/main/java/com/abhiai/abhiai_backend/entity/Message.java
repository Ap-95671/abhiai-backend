package com.abhiai.abhiai_backend.entity;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

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
import jakarta.persistence.Table;

@Entity
@Table(name = "messages", indexes = {
        @Index(name = "idx_messages_conversation_created_at", columnList = "conversation_id,created_at,id")
})
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MessageRole role;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "ai_provider", length = 64)
    private String aiProvider;
    @Column(name = "ai_model", length = 160)
    private String aiModel;
    @Column(name = "input_tokens")
    private Integer inputTokens;
    @Column(name = "output_tokens")
    private Integer outputTokens;
    @Column(name = "latency_ms")
    private Long latencyMs;
    @Column(name = "fallback_used")
    private Boolean fallbackUsed;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Message() {
    }

    public Message(Conversation conversation, MessageRole role, String content) {
        this.conversation = conversation;
        this.role = role;
        this.content = content;
    }

    public void applyAiMetadata(com.abhiai.abhiai_backend.ai.AiCompletion completion) {
        this.aiProvider = completion.provider();
        this.aiModel = completion.model();
        this.inputTokens = completion.inputTokens();
        this.outputTokens = completion.outputTokens();
        this.latencyMs = completion.latencyMs();
        this.fallbackUsed = completion.fallbackUsed();
    }
    

    public UUID getId() {
        return id;
    }

    public MessageRole getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
    public String getAiProvider() { return aiProvider; }
    public String getAiModel() { return aiModel; }
    public Integer getInputTokens() { return inputTokens; }
    public Integer getOutputTokens() { return outputTokens; }
    public Long getLatencyMs() { return latencyMs; }
    public Boolean getFallbackUsed() { return fallbackUsed; }
}
