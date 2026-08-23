package com.abhiai.abhiai_backend.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.abhiai.abhiai_backend.entity.PostRepost;

public interface PostRepostRepository extends JpaRepository<PostRepost, UUID> {

    boolean existsByPostIdAndUserId(UUID postId, UUID userId);

    Optional<PostRepost> findByPostIdAndUserId(UUID postId, UUID userId);

    @EntityGraph(attributePaths = "user")
    Page<PostRepost> findByPostId(UUID postId, Pageable pageable);
}
