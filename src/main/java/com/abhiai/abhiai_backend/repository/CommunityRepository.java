package com.abhiai.abhiai_backend.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import com.abhiai.abhiai_backend.entity.Community;
import com.abhiai.abhiai_backend.entity.CommunityPrivacy;

public interface CommunityRepository extends JpaRepository<Community, UUID> {

    @EntityGraph(attributePaths = "owner")
    Optional<Community> findBySlugIgnoreCase(String slug);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "owner")
    @Query("select community from Community community where lower(community.slug) = lower(:slug)")
    Optional<Community> findBySlugForUpdate(@Param("slug") String slug);

    boolean existsBySlugIgnoreCase(String slug);

    @EntityGraph(attributePaths = "owner")
    Page<Community> findAllByPrivacyOrderByMemberCountDescCreatedAtDescIdDesc(
            CommunityPrivacy privacy,
            Pageable pageable);
}
