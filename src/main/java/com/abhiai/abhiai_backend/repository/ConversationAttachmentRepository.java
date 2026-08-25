package com.abhiai.abhiai_backend.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.abhiai.abhiai_backend.entity.AiAttachmentStatus;
import com.abhiai.abhiai_backend.entity.ConversationAttachment;

public interface ConversationAttachmentRepository extends JpaRepository<ConversationAttachment, UUID> {

    List<ConversationAttachment> findAllByConversationIdOrderByCreatedAtAscIdAsc(UUID conversationId);

    List<ConversationAttachment> findAllByMessageId(UUID messageId);

    List<ConversationAttachment> findAllByIdInAndConversationIdAndProcessingStatus(
            Collection<UUID> ids,
            UUID conversationId,
            AiAttachmentStatus processingStatus);

    boolean existsByMediaAssetId(UUID mediaAssetId);
}
