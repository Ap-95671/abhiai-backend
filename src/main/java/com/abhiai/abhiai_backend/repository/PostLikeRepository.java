package com.abhiai.abhiai_backend.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.abhiai.abhiai_backend.entity.PostLike;

public interface PostLikeRepository extends JpaRepository<PostLike, UUID> {

    @Query(value = """
            select liked.post from PostLike liked
            where liked.user.id = :profileUserId
              and liked.post.deletedAt is null
              and (liked.post.community is null or liked.post.community.privacy = com.abhiai.abhiai_backend.entity.CommunityPrivacy.PUBLIC)
              and (
                liked.post.author.id = :viewerId
                or liked.post.visibility = com.abhiai.abhiai_backend.entity.PostVisibility.PUBLIC
                or (liked.post.visibility = com.abhiai.abhiai_backend.entity.PostVisibility.FOLLOWERS and exists (
                    select follow.id from Follow follow
                    where follow.follower.id = :viewerId and follow.following.id = liked.post.author.id
                ))
              )
            order by liked.createdAt desc, liked.id desc
            """, countQuery = """
            select count(liked) from PostLike liked
            where liked.user.id = :profileUserId
              and liked.post.deletedAt is null
              and (liked.post.community is null or liked.post.community.privacy = com.abhiai.abhiai_backend.entity.CommunityPrivacy.PUBLIC)
              and (
                liked.post.author.id = :viewerId
                or liked.post.visibility = com.abhiai.abhiai_backend.entity.PostVisibility.PUBLIC
                or (liked.post.visibility = com.abhiai.abhiai_backend.entity.PostVisibility.FOLLOWERS and exists (
                    select follow.id from Follow follow
                    where follow.follower.id = :viewerId and follow.following.id = liked.post.author.id
                ))
              )
            """)
    Page<com.abhiai.abhiai_backend.entity.Post> findVisibleProfileLikedPosts(
            @Param("viewerId") UUID viewerId,
            @Param("profileUserId") UUID profileUserId,
            Pageable pageable);

    boolean existsByPostIdAndUserId(UUID postId, UUID userId);

    Optional<PostLike> findByPostIdAndUserId(UUID postId, UUID userId);

    @EntityGraph(attributePaths = "user")
    Page<PostLike> findByPostId(UUID postId, Pageable pageable);
}
