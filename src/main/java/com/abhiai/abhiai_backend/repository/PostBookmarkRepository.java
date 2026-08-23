package com.abhiai.abhiai_backend.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.abhiai.abhiai_backend.entity.PostBookmark;
import com.abhiai.abhiai_backend.entity.PostVisibility;

public interface PostBookmarkRepository extends JpaRepository<PostBookmark, UUID> {

    boolean existsByPostIdAndUserId(UUID postId, UUID userId);

    Optional<PostBookmark> findByPostIdAndUserId(UUID postId, UUID userId);

    @EntityGraph(attributePaths = {"post", "post.author"})
    @Query(
            value = """
                    select bookmark
                    from PostBookmark bookmark
                    where bookmark.user.id = :userId
                      and bookmark.post.deletedAt is null
                      and (
                        bookmark.post.author.id = :userId
                        or bookmark.post.visibility = :publicVisibility
                        or (
                          bookmark.post.visibility = :followersVisibility
                          and exists (
                            select follow.id
                            from Follow follow
                            where follow.follower.id = :userId
                              and follow.following.id = bookmark.post.author.id
                          )
                        )
                      )
                    """,
            countQuery = """
                    select count(bookmark)
                    from PostBookmark bookmark
                    where bookmark.user.id = :userId
                      and bookmark.post.deletedAt is null
                      and (
                        bookmark.post.author.id = :userId
                        or bookmark.post.visibility = :publicVisibility
                        or (
                          bookmark.post.visibility = :followersVisibility
                          and exists (
                            select follow.id
                            from Follow follow
                            where follow.follower.id = :userId
                              and follow.following.id = bookmark.post.author.id
                          )
                        )
                      )
                    """)
    Page<PostBookmark> findAccessibleByUserId(
            @Param("userId") UUID userId,
            @Param("publicVisibility") PostVisibility publicVisibility,
            @Param("followersVisibility") PostVisibility followersVisibility,
            Pageable pageable);
}
