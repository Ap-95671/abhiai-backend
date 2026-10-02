package com.abhiai.abhiai_backend.entity;

import java.time.Instant;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
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
import jakarta.persistence.OrderColumn;
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

    @Column(name = "client_item_id", length = 128)
    private String clientItemId;

    public String getClientItemId() { return clientItemId; }
    public void setClientItemId(String value) { clientItemId = value; }

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

    @Column(name = "ai_request_id", length = 64) private String aiRequestId;
    @Column(name = "ai_intent", length = 32) private String aiIntent;
    @Column(name = "ai_task_type", length = 32) private String aiTaskType;
    @Column(name = "ai_complexity", length = 16) private String aiComplexity;
    @Column(name = "ai_strategy", length = 40) private String aiStrategy;
    @Column(name = "ai_quality_score") private Integer aiQualityScore;
    @Column(name = "ai_feedback") private Boolean aiFeedback;
    public String getAiRequestId() { return aiRequestId; }
    public String getAiIntent() { return aiIntent; }
    public String getAiTaskType() { return aiTaskType; }
    public String getAiStrategy() { return aiStrategy; }
    public Boolean getAiFeedback() { return aiFeedback; }
    public void recordAiFeedback(boolean positive) { aiFeedback = positive; }

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "message_citations", joinColumns = @JoinColumn(name = "message_id"))
    @OrderColumn(name = "position")
    private List<MessageCitation> citations = new ArrayList<>();

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
        var execution = completion.execution();
        if (execution != null) {
            this.aiRequestId = execution.requestId(); this.aiIntent = execution.intent().name();
            this.aiTaskType = execution.taskType().name(); this.aiComplexity = execution.complexity().name();
            this.aiStrategy = execution.strategy().name(); this.aiQualityScore = execution.qualityScore();
        }
    }

    public void replaceCitations(List<MessageCitation> values) {
        citations.clear();
        citations.addAll(values);
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
    public List<MessageCitation> getCitations() { return List.copyOf(citations); }
}
