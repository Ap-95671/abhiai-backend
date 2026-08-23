package com.abhiai.abhiai_backend.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.abhiai.abhiai_backend.entity.SocialNotification;
import com.abhiai.abhiai_backend.entity.NotificationType;

public interface SocialNotificationRepository extends JpaRepository<SocialNotification, UUID> {

    @EntityGraph(attributePaths = {"actor", "post"})
    Page<SocialNotification> findByRecipientId(UUID recipientId, Pageable pageable);

    @EntityGraph(attributePaths = {"actor", "post"})
    Page<SocialNotification> findByRecipientIdAndReadAtIsNull(UUID recipientId, Pageable pageable);

    @EntityGraph(attributePaths = {"actor", "post"})
    @Query("""
            select notification from SocialNotification notification
            where notification.recipient.id = :recipientId
              and not exists (select block.id from UserBlock block where
                (block.blocker.id = :recipientId and block.blocked.id = notification.actor.id)
                or (block.blocker.id = notification.actor.id and block.blocked.id = :recipientId))
            """)
    Page<SocialNotification> findVisible(@Param("recipientId") UUID recipientId, Pageable pageable);

    @EntityGraph(attributePaths = {"actor", "post"})
    @Query("""
            select notification from SocialNotification notification
            where notification.recipient.id = :recipientId and notification.readAt is null
              and not exists (select block.id from UserBlock block where
                (block.blocker.id = :recipientId and block.blocked.id = notification.actor.id)
                or (block.blocker.id = notification.actor.id and block.blocked.id = :recipientId))
            """)
    Page<SocialNotification> findVisibleUnread(@Param("recipientId") UUID recipientId, Pageable pageable);

    @EntityGraph(attributePaths = {"actor", "post"})
    Optional<SocialNotification> findByIdAndRecipientId(UUID notificationId, UUID recipientId);

    long countByRecipientIdAndReadAtIsNull(UUID recipientId);

    @Query("""
            select count(notification) from SocialNotification notification
            where notification.recipient.id = :recipientId and notification.readAt is null
              and not exists (select block.id from UserBlock block where
                (block.blocker.id = :recipientId and block.blocked.id = notification.actor.id)
                or (block.blocker.id = notification.actor.id and block.blocked.id = :recipientId))
            """)
    long countVisibleUnread(@Param("recipientId") UUID recipientId);

    @Query("""
            select count(notification) > 0
            from SocialNotification notification
            where notification.recipient.id = :recipientId
              and notification.actor.id = :actorId
              and notification.type = :type
              and ((:postId is null and notification.post is null)
                   or notification.post.id = :postId)
            """)
    boolean existsEquivalentEvent(
            @Param("recipientId") UUID recipientId,
            @Param("actorId") UUID actorId,
            @Param("type") NotificationType type,
            @Param("postId") UUID postId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update SocialNotification notification
            set notification.readAt = :readAt
            where notification.recipient.id = :recipientId
              and notification.readAt is null
            """)
    int markAllRead(
            @Param("recipientId") UUID recipientId,
            @Param("readAt") Instant readAt);
}
