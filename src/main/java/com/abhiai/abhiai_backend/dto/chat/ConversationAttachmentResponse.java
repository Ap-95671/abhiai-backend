package com.abhiai.abhiai_backend.dto.chat;

import java.time.Instant;
import java.util.UUID;

import com.abhiai.abhiai_backend.entity.AiAttachmentKind;
import com.abhiai.abhiai_backend.entity.AiAttachmentStatus;
import com.abhiai.abhiai_backend.entity.ConversationAttachment;

public record ConversationAttachmentResponse(
        UUID id,
        UUID mediaId,
        String filename,
        String contentType,
        long byteSize,
        AiAttachmentKind kind,
        AiAttachmentStatus processingStatus,
        String processingError,
        Instant createdAt,
        Instant processedAt) {

    public static ConversationAttachmentResponse from(ConversationAttachment attachment) {
        var media = attachment.getMediaAsset();
        return new ConversationAttachmentResponse(
                attachment.getId(),
                media.getId(),
                media.getOriginalFilename(),
                media.getContentType(),
                media.getByteSize(),
                attachment.getKind(),
                attachment.getProcessingStatus(),
                attachment.getProcessingError(),
                attachment.getCreatedAt(),
                attachment.getProcessedAt());
    }
}
