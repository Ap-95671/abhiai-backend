package com.abhiai.abhiai_backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.abhiai.abhiai_backend.entity.DirectMessage;

public interface DirectMessageRepository extends JpaRepository<DirectMessage, UUID> {

    @EntityGraph(attributePaths = "sender")
    Page<DirectMessage> findAllByConversationIdOrderByCreatedAtDescIdDesc(
            UUID conversationId,
            Pageable pageable);

    @EntityGraph(attributePaths = "sender")
    Optional<DirectMessage> findFirstByConversationIdOrderByCreatedAtDescIdDesc(UUID conversationId);

    @EntityGraph(attributePaths = "sender")
    Optional<DirectMessage> findByIdAndConversationId(UUID messageId, UUID conversationId);

    @EntityGraph(attributePaths = "sender")
    @Query("""
            select message
            from DirectMessage message
            where message.conversation.id = :conversationId
              and message.sender.id <> :userId
              and message.deletedAt is null
              and not exists (
                select receipt.id
                from MessageReadReceipt receipt
                where receipt.message.id = message.id
                  and receipt.user.id = :userId
              )
            order by message.createdAt asc, message.id asc
            """)
    List<DirectMessage> findUnreadForUser(
            @Param("conversationId") UUID conversationId,
            @Param("userId") UUID userId);

    @Query("""
            select count(message)
            from DirectMessage message
            where message.conversation.id = :conversationId
              and message.sender.id <> :userId
              and message.deletedAt is null
              and not exists (
                select receipt.id
                from MessageReadReceipt receipt
                where receipt.message.id = message.id
                  and receipt.user.id = :userId
              )
            """)
    long countUnreadForUser(
            @Param("conversationId") UUID conversationId,
            @Param("userId") UUID userId);
}
