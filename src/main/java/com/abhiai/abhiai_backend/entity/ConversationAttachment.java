package com.abhiai.abhiai_backend.entity;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "conversation_attachments")
public class ConversationAttachment {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id")
    private Message message;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "media_asset_id", nullable = false, unique = true)
    private MediaAsset mediaAsset;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AiAttachmentKind kind;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 20)
    private AiAttachmentStatus processingStatus;

    @Column(name = "extracted_text", columnDefinition = "text")
    private String extractedText;

    @Column(name = "processing_error", length = 500)
    private String processingError;

    @Column(columnDefinition = "text")
    private String embedding;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    protected ConversationAttachment() {
    }

    public ConversationAttachment(
            UUID id,
            Conversation conversation,
            MediaAsset mediaAsset,
            AiAttachmentKind kind) {
        this.id = id;
        this.conversation = conversation;
        this.mediaAsset = mediaAsset;
        this.kind = kind;
        this.processingStatus = kind == AiAttachmentKind.IMAGE
                ? AiAttachmentStatus.READY
                : AiAttachmentStatus.PENDING;
        if (this.processingStatus == AiAttachmentStatus.READY) {
            this.processedAt = Instant.now();
        }
    }

    public void attachToMessage(Message message) {
        this.message = message;
    }

    public void processingCompleted(String extractedText, String embedding) {
        this.extractedText = extractedText;
        this.embedding = embedding;
        this.processingStatus = AiAttachmentStatus.READY;
        this.processingError = null;
        this.processedAt = Instant.now();
    }

    public void processingFailed(String error) {
        this.processingStatus = AiAttachmentStatus.FAILED;
        this.processingError = error;
        this.processedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public Conversation getConversation() { return conversation; }
    public Message getMessage() { return message; }
    public MediaAsset getMediaAsset() { return mediaAsset; }
    public AiAttachmentKind getKind() { return kind; }
    public AiAttachmentStatus getProcessingStatus() { return processingStatus; }
    public String getExtractedText() { return extractedText; }
    public String getProcessingError() { return processingError; }
    public String getEmbedding() { return embedding; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getProcessedAt() { return processedAt; }
}
