package com.abhiai.abhiai_backend.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.abhiai.abhiai_backend.entity.Post;
import com.abhiai.abhiai_backend.entity.PostVisibility;

public interface PostRepository extends JpaRepository<Post, UUID> {

    @EntityGraph(attributePaths = {"author", "media", "community"})
    @Query(value = """
            select post from Post post
            where post.author.id = :profileUserId
              and post.deletedAt is null
              and not exists (select block.id from UserBlock block where
                (block.blocker.id = :viewerId and block.blocked.id = post.author.id)
                or (block.blocker.id = post.author.id and block.blocked.id = :viewerId))
              and (post.community is null or post.community.privacy = com.abhiai.abhiai_backend.entity.CommunityPrivacy.PUBLIC)
              and (
                post.author.id = :viewerId
                or post.visibility = com.abhiai.abhiai_backend.entity.PostVisibility.PUBLIC
                or (post.visibility = com.abhiai.abhiai_backend.entity.PostVisibility.FOLLOWERS and exists (
                    select follow.id from Follow follow
                    where follow.follower.id = :viewerId and follow.following.id = post.author.id
                ))
              )
            order by post.pinnedAt desc, post.createdAt desc, post.id desc
            """, countQuery = """
            select count(post) from Post post
            where post.author.id = :profileUserId
              and post.deletedAt is null
              and not exists (select block.id from UserBlock block where
                (block.blocker.id = :viewerId and block.blocked.id = post.author.id)
                or (block.blocker.id = post.author.id and block.blocked.id = :viewerId))
              and (post.community is null or post.community.privacy = com.abhiai.abhiai_backend.entity.CommunityPrivacy.PUBLIC)
              and (
                post.author.id = :viewerId
                or post.visibility = com.abhiai.abhiai_backend.entity.PostVisibility.PUBLIC
                or (post.visibility = com.abhiai.abhiai_backend.entity.PostVisibility.FOLLOWERS and exists (
                    select follow.id from Follow follow
                    where follow.follower.id = :viewerId and follow.following.id = post.author.id
                ))
              )
            """)
    Page<Post> findVisibleProfilePosts(
            @Param("viewerId") UUID viewerId,
            @Param("profileUserId") UUID profileUserId,
            Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Post post
            set post.pinnedAt = null
            where post.author.id = :authorId
              and post.pinnedAt is not null
            """)
    int clearPinnedPostByAuthorId(@Param("authorId") UUID authorId);

    @EntityGraph(attributePaths = {"author", "media", "community"})
    @Query(value = """
            select post from Post post
            where post.author.id = :profileUserId
              and post.deletedAt is null
              and not exists (select block.id from UserBlock block where
                (block.blocker.id = :viewerId and block.blocked.id = post.author.id)
                or (block.blocker.id = post.author.id and block.blocked.id = :viewerId))
              and exists (select media.id from MediaAsset media where media.post.id = post.id)
              and (post.community is null or post.community.privacy = com.abhiai.abhiai_backend.entity.CommunityPrivacy.PUBLIC)
              and (
                post.author.id = :viewerId
                or post.visibility = com.abhiai.abhiai_backend.entity.PostVisibility.PUBLIC
                or (post.visibility = com.abhiai.abhiai_backend.entity.PostVisibility.FOLLOWERS and exists (
                    select follow.id from Follow follow
                    where follow.follower.id = :viewerId and follow.following.id = post.author.id
                ))
              )
            order by post.createdAt desc, post.id desc
            """, countQuery = """
            select count(post) from Post post
            where post.author.id = :profileUserId
              and post.deletedAt is null
              and not exists (select block.id from UserBlock block where
                (block.blocker.id = :viewerId and block.blocked.id = post.author.id)
                or (block.blocker.id = post.author.id and block.blocked.id = :viewerId))
              and exists (select media.id from MediaAsset media where media.post.id = post.id)
              and (post.community is null or post.community.privacy = com.abhiai.abhiai_backend.entity.CommunityPrivacy.PUBLIC)
              and (
                post.author.id = :viewerId
                or post.visibility = com.abhiai.abhiai_backend.entity.PostVisibility.PUBLIC
                or (post.visibility = com.abhiai.abhiai_backend.entity.PostVisibility.FOLLOWERS and exists (
                    select follow.id from Follow follow
                    where follow.follower.id = :viewerId and follow.following.id = post.author.id
                ))
              )
            """)
    Page<Post> findVisibleProfileMediaPosts(
            @Param("viewerId") UUID viewerId,
            @Param("profileUserId") UUID profileUserId,
            Pageable pageable);

    @EntityGraph(attributePaths = {"author", "media", "community"})
    @Query(
            value = """
                    select post
                    from Post post
                    where post.community.id = :communityId
                      and post.deletedAt is null
                      and not exists (select block.id from UserBlock block where
                        (block.blocker.id = :viewerId and block.blocked.id = post.author.id)
                        or (block.blocker.id = post.author.id and block.blocked.id = :viewerId))
                      and post.visibility = com.abhiai.abhiai_backend.entity.PostVisibility.PUBLIC
                    order by post.createdAt desc, post.id desc
                    """,
            countQuery = """
                    select count(post)
                    from Post post
                    where post.community.id = :communityId
                      and post.deletedAt is null
                      and not exists (select block.id from UserBlock block where
                        (block.blocker.id = :viewerId and block.blocked.id = post.author.id)
                        or (block.blocker.id = post.author.id and block.blocked.id = :viewerId))
                      and post.visibility = com.abhiai.abhiai_backend.entity.PostVisibility.PUBLIC
                    """)
    Page<Post> findCommunityFeed(@Param("communityId") UUID communityId,
            @Param("viewerId") UUID viewerId, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "media"})
    @Query("""
            select post
            from Post post
            where post.deletedAt is null
              and post.visibility = com.abhiai.abhiai_backend.entity.PostVisibility.PUBLIC
              and post.createdAt >= :cutoff
            order by (post.likeCount * 3 + post.replyCount * 4
                      + post.repostCount * 5 + post.viewCount) desc,
                     post.createdAt desc,
                     post.id desc
            """)
    List<Post> findTrendingPublicPosts(@Param("cutoff") Instant cutoff, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "media"})
    @Query("""
            select post
            from Post post
            where post.deletedAt is null
              and post.visibility = com.abhiai.abhiai_backend.entity.PostVisibility.PUBLIC
              and post.createdAt >= :cutoff
              and post.replyCount > 0
            order by (post.replyCount * 5 + post.likeCount * 2
                      + post.repostCount * 3 + post.viewCount) desc,
                     post.createdAt desc,
                     post.id desc
            """)
    List<Post> findPopularPublicDiscussions(@Param("cutoff") Instant cutoff, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "media"})
    @Query("""
            select post
            from Post post
            where post.deletedAt is null
              and post.visibility = com.abhiai.abhiai_backend.entity.PostVisibility.PUBLIC
              and post.createdAt >= :cutoff
              and exists (
                select media.id
                from MediaAsset media
                where media.post.id = post.id
              )
            order by (post.likeCount * 3 + post.replyCount * 4
                      + post.repostCount * 5 + post.viewCount) desc,
                     post.createdAt desc,
                     post.id desc
            """)
    List<Post> findRecommendedPublicMedia(@Param("cutoff") Instant cutoff, Pageable pageable);

    @EntityGraph(attributePaths = "author")
    Optional<Post> findByIdAndDeletedAtIsNull(UUID postId);

    @EntityGraph(attributePaths = "author")
    @Query(
            value = """
                    select post
                    from Post post
                    where post.deletedAt is null
                      and not exists (select block.id from UserBlock block where
                        (block.blocker.id = :userId and block.blocked.id = post.author.id)
                        or (block.blocker.id = post.author.id and block.blocked.id = :userId))
                      and (
                        post.author.id = :userId
                        or (
                          post.visibility in :followedVisibilities
                          and exists (
                            select follow.id
                            from Follow follow
                            where follow.follower.id = :userId
                              and follow.following.id = post.author.id
                          )
                        )
                      )
                    """,
            countQuery = """
                    select count(post)
                    from Post post
                    where post.deletedAt is null
                      and not exists (select block.id from UserBlock block where
                        (block.blocker.id = :userId and block.blocked.id = post.author.id)
                        or (block.blocker.id = post.author.id and block.blocked.id = :userId))
                      and (
                        post.author.id = :userId
                        or (
                          post.visibility in :followedVisibilities
                          and exists (
                            select follow.id
                            from Follow follow
                            where follow.follower.id = :userId
                              and follow.following.id = post.author.id
                          )
                        )
                      )
                    """)
    Page<Post> findChronologicalHomeFeed(
            @Param("userId") UUID userId,
            @Param("followedVisibilities") Set<PostVisibility> followedVisibilities,
            Pageable pageable);

    @EntityGraph(attributePaths = "author")
    @Query(
            value = """
                    select post
                    from Post post
                    where post.deletedAt is null
                      and not exists (select block.id from UserBlock block where
                        (block.blocker.id = :userId and block.blocked.id = post.author.id)
                        or (block.blocker.id = post.author.id and block.blocked.id = :userId))
                      and exists (
                        select media.id
                        from MediaAsset media
                        where media.post.id = post.id
                          and media.contentType in ('video/mp4', 'video/webm')
                      )
                      and (
                        post.author.id = :userId
                        or post.visibility = com.abhiai.abhiai_backend.entity.PostVisibility.PUBLIC
                        or (
                          post.visibility = com.abhiai.abhiai_backend.entity.PostVisibility.FOLLOWERS
                          and exists (
                            select follow.id
                            from Follow follow
                            where follow.follower.id = :userId
                              and follow.following.id = post.author.id
                          )
                        )
                      )
                    """,
            countQuery = """
                    select count(post)
                    from Post post
                    where post.deletedAt is null
                      and not exists (select block.id from UserBlock block where
                        (block.blocker.id = :userId and block.blocked.id = post.author.id)
                        or (block.blocker.id = post.author.id and block.blocked.id = :userId))
                      and exists (
                        select media.id
                        from MediaAsset media
                        where media.post.id = post.id
                          and media.contentType in ('video/mp4', 'video/webm')
                      )
                      and (
                        post.author.id = :userId
                        or post.visibility = com.abhiai.abhiai_backend.entity.PostVisibility.PUBLIC
                        or (
                          post.visibility = com.abhiai.abhiai_backend.entity.PostVisibility.FOLLOWERS
                          and exists (
                            select follow.id
                            from Follow follow
                            where follow.follower.id = :userId
                              and follow.following.id = post.author.id
                          )
                        )
                      )
                    """)
    Page<Post> findVideoFeed(@Param("userId") UUID userId, Pageable pageable);

    @EntityGraph(attributePaths = "author")
    @Query(
            value = """
                    select post
                    from Post post
                    where post.deletedAt is null
                      and exists (
                        select relation.id
                        from PostHashtag relation
                        where relation.post.id = post.id
                          and relation.hashtag.id = :hashtagId
                      )
                      and (
                        post.author.id = :userId
                        or post.visibility = com.abhiai.abhiai_backend.entity.PostVisibility.PUBLIC
                        or (
                          post.visibility = com.abhiai.abhiai_backend.entity.PostVisibility.FOLLOWERS
                          and exists (
                            select follow.id from Follow follow
                            where follow.follower.id = :userId
                              and follow.following.id = post.author.id
                          )
                        )
                      )
                    """,
            countQuery = """
                    select count(post)
                    from Post post
                    where post.deletedAt is null
                      and exists (
                        select relation.id
                        from PostHashtag relation
                        where relation.post.id = post.id
                          and relation.hashtag.id = :hashtagId
                      )
                      and (
                        post.author.id = :userId
                        or post.visibility = com.abhiai.abhiai_backend.entity.PostVisibility.PUBLIC
                        or (
                          post.visibility = com.abhiai.abhiai_backend.entity.PostVisibility.FOLLOWERS
                          and exists (
                            select follow.id from Follow follow
                            where follow.follower.id = :userId
                              and follow.following.id = post.author.id
                          )
                        )
                      )
                    """)
    Page<Post> findVisiblePostsByHashtag(
            @Param("userId") UUID userId,
            @Param("hashtagId") UUID hashtagId,
            Pageable pageable);

    @Query(
            value = """
                    select post.*
                    from posts post
                    join users author on author.id = post.author_id
                    where post.deleted_at is null
                      and post.search_vector @@ websearch_to_tsquery('simple', :query)
                      and (:authorUsername is null or author.username = :authorUsername)
                      and (cast(:fromDate as timestamp with time zone) is null
                           or post.created_at >= cast(:fromDate as timestamp with time zone))
                      and (cast(:toDate as timestamp with time zone) is null
                           or post.created_at <= cast(:toDate as timestamp with time zone))
                      and (
                        :hasMedia is null
                        or (:hasMedia = true and exists (
                          select 1 from media_assets media where media.post_id = post.id
                        ))
                        or (:hasMedia = false and not exists (
                          select 1 from media_assets media where media.post_id = post.id
                        ))
                      )
                      and (
                        post.author_id = :userId
                        or post.visibility = 'PUBLIC'
                        or (
                          post.visibility = 'FOLLOWERS'
                          and exists (
                            select 1
                            from user_follows follow_relation
                            where follow_relation.follower_id = :userId
                              and follow_relation.following_id = post.author_id
                          )
                        )
                      )
                    order by
                             case when :sortMode = 'RELEVANCE'
                                  then ts_rank(post.search_vector, websearch_to_tsquery('simple', :query)) end desc,
                             case when :sortMode = 'POPULAR'
                                  then post.like_count + (post.reply_count * 2) + (post.repost_count * 3) + (post.view_count * 0.05) end desc,
                             post.created_at desc,
                             post.id desc
                    """,
            countQuery = """
                    select count(*)
                    from posts post
                    join users author on author.id = post.author_id
                    where post.deleted_at is null
                      and post.search_vector @@ websearch_to_tsquery('simple', :query)
                      and (:authorUsername is null or author.username = :authorUsername)
                      and (cast(:fromDate as timestamp with time zone) is null
                           or post.created_at >= cast(:fromDate as timestamp with time zone))
                      and (cast(:toDate as timestamp with time zone) is null
                           or post.created_at <= cast(:toDate as timestamp with time zone))
                      and (
                        :hasMedia is null
                        or (:hasMedia = true and exists (
                          select 1 from media_assets media where media.post_id = post.id
                        ))
                        or (:hasMedia = false and not exists (
                          select 1 from media_assets media where media.post_id = post.id
                        ))
                      )
                      and (
                        post.author_id = :userId
                        or post.visibility = 'PUBLIC'
                        or (
                          post.visibility = 'FOLLOWERS'
                          and exists (
                            select 1
                            from user_follows follow_relation
                            where follow_relation.follower_id = :userId
                              and follow_relation.following_id = post.author_id
                          )
                        )
                      )
                    """,
            nativeQuery = true)
    Page<Post> searchVisiblePosts(
            @Param("userId") UUID userId,
            @Param("query") String query,
            @Param("authorUsername") String authorUsername,
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate,
            @Param("hasMedia") Boolean hasMedia,
            @Param("sortMode") String sortMode,
            Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Post post set post.likeCount = post.likeCount + 1 where post.id = :postId")
    int incrementLikeCount(@Param("postId") UUID postId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Post post
            set post.likeCount = case when post.likeCount > 0 then post.likeCount - 1 else 0 end
            where post.id = :postId
            """)
    int decrementLikeCount(@Param("postId") UUID postId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Post post set post.replyCount = post.replyCount + 1 where post.id = :postId")
    int incrementReplyCount(@Param("postId") UUID postId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Post post
            set post.replyCount = case when post.replyCount > 0 then post.replyCount - 1 else 0 end
            where post.id = :postId
            """)
    int decrementReplyCount(@Param("postId") UUID postId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Post post set post.repostCount = post.repostCount + 1 where post.id = :postId")
    int incrementRepostCount(@Param("postId") UUID postId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Post post
            set post.repostCount = case when post.repostCount > 0 then post.repostCount - 1 else 0 end
            where post.id = :postId
            """)
    int decrementRepostCount(@Param("postId") UUID postId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Post post set post.bookmarkCount = post.bookmarkCount + 1 where post.id = :postId")
    int incrementBookmarkCount(@Param("postId") UUID postId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Post post
            set post.bookmarkCount =
                case when post.bookmarkCount > 0 then post.bookmarkCount - 1 else 0 end
            where post.id = :postId
            """)
    int decrementBookmarkCount(@Param("postId") UUID postId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Post post set post.viewCount = post.viewCount + 1 where post.id = :postId")
    int incrementViewCount(@Param("postId") UUID postId);
}
