package com.abhiai.abhiai_backend.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.abhiai.abhiai_backend.entity.Story;

public interface StoryRepository extends JpaRepository<Story, UUID> {

    @EntityGraph(attributePaths = {"author", "author.profileMedia", "media"})
    Page<Story> findByExpiresAtAfter(Instant now, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "author.profileMedia", "media"})
    Optional<Story> findByIdAndExpiresAtAfter(UUID storyId, Instant now);

    Optional<Story> findByMediaIdAndExpiresAtAfter(UUID mediaId, Instant now);

    boolean existsByMediaId(UUID mediaId);

    @EntityGraph(attributePaths = "media")
    List<Story> findTop100ByExpiresAtBeforeOrderByExpiresAtAsc(Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Story story set story.viewCount = story.viewCount + 1 where story.id = :storyId")
    int incrementViewCount(@Param("storyId") UUID storyId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Story story set story.reactionCount = story.reactionCount + 1 where story.id = :storyId")
    int incrementReactionCount(@Param("storyId") UUID storyId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Story story
            set story.reactionCount = case when story.reactionCount > 0 then story.reactionCount - 1 else 0 end
            where story.id = :storyId
            """)
    int decrementReactionCount(@Param("storyId") UUID storyId);
}
