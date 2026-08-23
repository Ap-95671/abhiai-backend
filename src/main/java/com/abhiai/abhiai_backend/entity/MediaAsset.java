package com.abhiai.abhiai_backend.entity;

import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import jakarta.persistence.*;

@Entity
@Table(name = "media_assets")
public class MediaAsset {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "owner_id", nullable = false) private User owner;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "post_id") private Post post;
    @Column(name = "storage_key", nullable = false, unique = true, length = 120) private String storageKey;
    @Column(name = "original_filename", nullable = false, length = 255) private String originalFilename;
    @Column(name = "content_type", nullable = false, length = 50) private String contentType;
    @Column(name = "byte_size", nullable = false) private long byteSize;
    @Column(name = "optimized_storage_key", length = 120) private String optimizedStorageKey;
    @Column(name = "thumbnail_storage_key", length = 120) private String thumbnailStorageKey;
    @Enumerated(EnumType.STRING) @Column(name = "processing_status", nullable = false, length = 20) private MediaProcessingStatus processingStatus;
    @Column(name = "processed_at") private Instant processedAt;
    private Short position;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    protected MediaAsset() {}
    public MediaAsset(UUID id, User owner, String storageKey, String originalFilename, String contentType, long byteSize) {
        this.id=id; this.owner=owner; this.storageKey=storageKey; this.originalFilename=originalFilename; this.contentType=contentType; this.byteSize=byteSize;
        this.processingStatus=(contentType.equals("image/jpeg")||contentType.equals("image/png"))?MediaProcessingStatus.PENDING:MediaProcessingStatus.NOT_REQUIRED;
    }
    public void attachTo(Post post, short position) { this.post=post; this.position=position; }
    public UUID getId(){return id;} public User getOwner(){return owner;} public Post getPost(){return post;}
    public String getStorageKey(){return storageKey;} public String getOriginalFilename(){return originalFilename;}
    public String getContentType(){return contentType;} public long getByteSize(){return byteSize;} public Short getPosition(){return position;} public Instant getCreatedAt(){return createdAt;}
    public String getOptimizedStorageKey(){return optimizedStorageKey;} public String getThumbnailStorageKey(){return thumbnailStorageKey;} public MediaProcessingStatus getProcessingStatus(){return processingStatus;} public Instant getProcessedAt(){return processedAt;}
    public String deliveryStorageKey(){return optimizedStorageKey==null?storageKey:optimizedStorageKey;}
    public void processingCompleted(String optimizedKey,String thumbnailKey){this.optimizedStorageKey=optimizedKey;this.thumbnailStorageKey=thumbnailKey;this.processingStatus=MediaProcessingStatus.COMPLETED;this.processedAt=Instant.now();}
    public void processingFailed(){this.processingStatus=MediaProcessingStatus.FAILED;this.processedAt=Instant.now();}
}
