package com.abhiai.abhiai_backend.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.abhiai.abhiai_backend.entity.Follow;

public interface FollowRepository extends JpaRepository<Follow, UUID> {

    boolean existsByFollowerIdAndFollowingId(UUID followerId, UUID followingId);

    Optional<Follow> findByFollowerIdAndFollowingId(UUID followerId, UUID followingId);

    @EntityGraph(attributePaths = "follower")
    Page<Follow> findByFollowingId(UUID followingId, Pageable pageable);

    @EntityGraph(attributePaths = "following")
    Page<Follow> findByFollowerId(UUID followerId, Pageable pageable);
}
