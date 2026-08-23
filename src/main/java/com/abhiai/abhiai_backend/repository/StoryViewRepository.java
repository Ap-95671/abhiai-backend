package com.abhiai.abhiai_backend.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.abhiai.abhiai_backend.entity.StoryView;

public interface StoryViewRepository extends JpaRepository<StoryView, UUID> {
    @Modifying(flushAutomatically = true)
    @Query(value = """
            insert into story_views (id, story_id, viewer_id, created_at)
            values (:id, :storyId, :viewerId, current_timestamp)
            on conflict (story_id, viewer_id) do nothing
            """, nativeQuery = true)
    int insertIfAbsent(@Param("id") UUID id, @Param("storyId") UUID storyId, @Param("viewerId") UUID viewerId);

    @Query("select view.story.id from StoryView view where view.viewer.id = :viewerId and view.story.id in :storyIds")
    List<UUID> findViewedStoryIds(@Param("viewerId") UUID viewerId, @Param("storyIds") Collection<UUID> storyIds);
}
