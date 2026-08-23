package com.abhiai.abhiai_backend.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.abhiai.abhiai_backend.entity.CommunityMembership;

public interface CommunityMembershipRepository extends JpaRepository<CommunityMembership, UUID> {

    boolean existsByCommunityIdAndUserId(UUID communityId, UUID userId);

    @EntityGraph(attributePaths = {"community", "user"})
    Optional<CommunityMembership> findByCommunityIdAndUserId(UUID communityId, UUID userId);

    @EntityGraph(attributePaths = "community")
    List<CommunityMembership> findAllByUserIdAndCommunityIdIn(
            UUID userId,
            Collection<UUID> communityIds);
}
