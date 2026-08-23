package com.abhiai.abhiai_backend.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.abhiai.abhiai_backend.entity.GroupParticipant;

public interface GroupParticipantRepository extends JpaRepository<GroupParticipant, UUID> {

    @EntityGraph(attributePaths = {"conversation", "user"})
    Optional<GroupParticipant> findByConversationIdAndUserId(UUID conversationId, UUID userId);

    boolean existsByConversationIdAndUserId(UUID conversationId, UUID userId);

    long countByConversationId(UUID conversationId);
}
