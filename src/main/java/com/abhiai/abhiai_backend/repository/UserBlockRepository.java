package com.abhiai.abhiai_backend.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.abhiai.abhiai_backend.entity.UserBlock;

public interface UserBlockRepository extends JpaRepository<UserBlock, UUID> {
    boolean existsByBlockerIdAndBlockedId(UUID blockerId, UUID blockedId);
    Optional<UserBlock> findByBlockerIdAndBlockedId(UUID blockerId, UUID blockedId);

    @Query("""
            select count(block) > 0 from UserBlock block
            where (block.blocker.id = :firstId and block.blocked.id = :secondId)
               or (block.blocker.id = :secondId and block.blocked.id = :firstId)
            """)
    boolean existsEitherDirection(@Param("firstId") UUID firstId, @Param("secondId") UUID secondId);

    @EntityGraph(attributePaths = "blocked")
    Page<UserBlock> findByBlockerId(UUID blockerId, Pageable pageable);
}
