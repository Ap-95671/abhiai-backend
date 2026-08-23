package com.abhiai.abhiai_backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.abhiai.abhiai_backend.entity.GroupConversation;

public interface GroupConversationRepository extends JpaRepository<GroupConversation, UUID> {

    @EntityGraph(attributePaths = {"owner", "participants.user"})
    @Query("""
            select distinct conversation
            from GroupConversation conversation
            join conversation.participants participant
            where participant.user.id = :userId
            order by conversation.updatedAt desc, conversation.id desc
            """)
    List<GroupConversation> findAllForUser(@Param("userId") UUID userId);

    @EntityGraph(attributePaths = {"owner", "participants.user"})
    @Query("""
            select distinct conversation
            from GroupConversation conversation
            join conversation.participants participant
            where conversation.id = :conversationId
              and participant.user.id = :userId
            """)
    Optional<GroupConversation> findAccessible(
            @Param("conversationId") UUID conversationId,
            @Param("userId") UUID userId);

    @EntityGraph(attributePaths = {"owner", "participants.user"})
    @Query("select conversation from GroupConversation conversation where conversation.id = :conversationId")
    Optional<GroupConversation> findDetailedById(@Param("conversationId") UUID conversationId);
}
