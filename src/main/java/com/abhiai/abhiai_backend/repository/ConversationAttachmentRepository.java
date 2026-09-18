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

    @org.springframework.data.jpa.repository.Query("select a from ConversationAttachment a where a.conversation.user.id=:userId and a.kind=com.abhiai.abhiai_backend.entity.AiAttachmentKind.DOCUMENT and lower(a.mediaAsset.originalFilename) like lower(concat('%',:query,'%')) order by a.createdAt desc")
    List<ConversationAttachment> searchOwned(UUID userId,String query,org.springframework.data.domain.Pageable page);

    boolean existsByMediaAssetId(UUID mediaAssetId);
}
