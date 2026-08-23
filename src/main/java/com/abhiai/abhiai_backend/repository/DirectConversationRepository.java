package com.abhiai.abhiai_backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.abhiai.abhiai_backend.entity.DirectConversation;

public interface DirectConversationRepository extends JpaRepository<DirectConversation, UUID> {

    @EntityGraph(attributePaths = "participants.user")
    Optional<DirectConversation> findByParticipantKey(String participantKey);

    @EntityGraph(attributePaths = "participants.user")
    @Query("""
            select distinct conversation
            from DirectConversation conversation
            join conversation.participants participant
            where participant.user.id = :userId
            order by conversation.updatedAt desc, conversation.id desc
            """)
    List<DirectConversation> findAllForUser(@Param("userId") UUID userId);

    @EntityGraph(attributePaths = "participants.user")
    @Query("""
            select distinct conversation
            from DirectConversation conversation
            join conversation.participants participant
            where conversation.id = :conversationId
              and participant.user.id = :userId
            """)
    Optional<DirectConversation> findAccessible(
            @Param("conversationId") UUID conversationId,
            @Param("userId") UUID userId);
}
