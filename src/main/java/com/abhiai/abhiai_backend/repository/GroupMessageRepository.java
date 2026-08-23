package com.abhiai.abhiai_backend.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.abhiai.abhiai_backend.entity.GroupMessage;

public interface GroupMessageRepository extends JpaRepository<GroupMessage, UUID> {

    @EntityGraph(attributePaths = "sender")
    Page<GroupMessage> findAllByConversationIdOrderByCreatedAtDescIdDesc(
            UUID conversationId,
            Pageable pageable);

    @EntityGraph(attributePaths = "sender")
    Optional<GroupMessage> findFirstByConversationIdOrderByCreatedAtDescIdDesc(UUID conversationId);

    @EntityGraph(attributePaths = "sender")
    Optional<GroupMessage> findByIdAndConversationId(UUID messageId, UUID conversationId);
}
