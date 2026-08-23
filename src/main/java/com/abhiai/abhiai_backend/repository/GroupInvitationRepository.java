package com.abhiai.abhiai_backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.abhiai.abhiai_backend.entity.GroupInvitation;
import com.abhiai.abhiai_backend.entity.GroupInvitationStatus;

public interface GroupInvitationRepository extends JpaRepository<GroupInvitation, UUID> {

    @EntityGraph(attributePaths = {"conversation", "conversation.owner", "inviter", "invitee"})
    Optional<GroupInvitation> findByConversationIdAndInviteeId(UUID conversationId, UUID inviteeId);

    @EntityGraph(attributePaths = {"conversation", "conversation.owner", "inviter", "invitee"})
    List<GroupInvitation> findAllByInviteeIdAndStatusOrderByCreatedAtDesc(
            UUID inviteeId,
            GroupInvitationStatus status);

    @EntityGraph(attributePaths = {"conversation", "conversation.owner", "inviter", "invitee"})
    Optional<GroupInvitation> findByIdAndInviteeId(UUID invitationId, UUID inviteeId);
}
