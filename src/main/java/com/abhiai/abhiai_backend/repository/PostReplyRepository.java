package com.abhiai.abhiai_backend.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.abhiai.abhiai_backend.entity.PostReply;

public interface PostReplyRepository extends JpaRepository<PostReply, UUID> {

    @EntityGraph(attributePaths = {"author", "post", "post.author", "post.media", "post.community"})
    @Query(value = """
            select reply from PostReply reply
            where reply.author.id = :profileUserId
              and reply.deletedAt is null
              and reply.post.deletedAt is null
              and (reply.post.community is null or reply.post.community.privacy = com.abhiai.abhiai_backend.entity.CommunityPrivacy.PUBLIC)
              and (
                reply.post.author.id = :viewerId
                or reply.post.visibility = com.abhiai.abhiai_backend.entity.PostVisibility.PUBLIC
                or (reply.post.visibility = com.abhiai.abhiai_backend.entity.PostVisibility.FOLLOWERS and exists (
                    select follow.id from Follow follow
                    where follow.follower.id = :viewerId and follow.following.id = reply.post.author.id
                ))
              )
            order by reply.createdAt desc, reply.id desc
            """, countQuery = """
            select count(reply) from PostReply reply
            where reply.author.id = :profileUserId
              and reply.deletedAt is null
              and reply.post.deletedAt is null
              and (reply.post.community is null or reply.post.community.privacy = com.abhiai.abhiai_backend.entity.CommunityPrivacy.PUBLIC)
              and (
                reply.post.author.id = :viewerId
                or reply.post.visibility = com.abhiai.abhiai_backend.entity.PostVisibility.PUBLIC
                or (reply.post.visibility = com.abhiai.abhiai_backend.entity.PostVisibility.FOLLOWERS and exists (
                    select follow.id from Follow follow
                    where follow.follower.id = :viewerId and follow.following.id = reply.post.author.id
                ))
              )
            """)
    Page<PostReply> findVisibleProfileReplies(
            @Param("viewerId") UUID viewerId,
            @Param("profileUserId") UUID profileUserId,
            Pageable pageable);

    @EntityGraph(attributePaths = "author")
    Page<PostReply> findByPostIdAndDeletedAtIsNull(UUID postId, Pageable pageable);

    @EntityGraph(attributePaths = "author")
    Optional<PostReply> findByIdAndPostIdAndDeletedAtIsNull(UUID replyId, UUID postId);

    @EntityGraph(attributePaths = {"author", "post", "post.author", "post.community"})
    Optional<PostReply> findByIdAndDeletedAtIsNull(UUID replyId);
}
