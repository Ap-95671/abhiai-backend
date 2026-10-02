package com.abhiai.abhiai_backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.abhiai.abhiai_backend.entity.Message;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select m from Message m where m.id = :messageId and m.conversation.id = :conversationId and m.conversation.user.id = :userId")
    java.util.Optional<Message> findOwnedForFeedback(UUID messageId, UUID conversationId, UUID userId);

    List<Message> findAllByConversationIdOrderByCreatedAtAscIdAsc(UUID conversationId);
    List<Message> findByConversationIdOrderByCreatedAtDescIdDesc(UUID conversationId, org.springframework.data.domain.Pageable pageable);
}
