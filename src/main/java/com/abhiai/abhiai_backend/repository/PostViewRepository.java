package com.abhiai.abhiai_backend.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.abhiai.abhiai_backend.entity.PostView;

public interface PostViewRepository extends JpaRepository<PostView, UUID> {

    @Modifying(flushAutomatically = true)
    @Query(
            value = """
                    insert into post_views (id, post_id, viewer_id, created_at)
                    values (:id, :postId, :viewerId, current_timestamp)
                    on conflict (post_id, viewer_id) do nothing
                    """,
            nativeQuery = true)
    int insertIfAbsent(
            @Param("id") UUID id,
            @Param("postId") UUID postId,
            @Param("viewerId") UUID viewerId);
}
