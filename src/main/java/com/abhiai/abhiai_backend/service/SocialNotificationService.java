package com.abhiai.abhiai_backend.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.abhiai.abhiai_backend.dto.common.PageResponse;
import com.abhiai.abhiai_backend.dto.notification.MarkAllNotificationsReadResponse;
import com.abhiai.abhiai_backend.dto.notification.NotificationResponse;
import com.abhiai.abhiai_backend.dto.notification.UnreadNotificationCountResponse;
import com.abhiai.abhiai_backend.entity.NotificationType;
import com.abhiai.abhiai_backend.entity.Post;
import com.abhiai.abhiai_backend.entity.SocialNotification;
import com.abhiai.abhiai_backend.entity.User;
import com.abhiai.abhiai_backend.exception.NotificationNotFoundException;
import com.abhiai.abhiai_backend.exception.UserNotFoundException;
import com.abhiai.abhiai_backend.repository.SocialNotificationRepository;
import com.abhiai.abhiai_backend.repository.UserRepository;

@Service
public class SocialNotificationService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Sort NOTIFICATION_SORT = Sort.by(
            Sort.Order.desc("createdAt"),
            Sort.Order.desc("id"));

    private final SocialNotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final BlockPolicyService blockPolicyService;
    private final MuteService muteService;

    public SocialNotificationService(
            SocialNotificationRepository notificationRepository,
            UserRepository userRepository,
            ApplicationEventPublisher eventPublisher) {
        this(notificationRepository, userRepository, eventPublisher, null);
    }

    public SocialNotificationService(SocialNotificationRepository notificationRepository,
            UserRepository userRepository, ApplicationEventPublisher eventPublisher,
            BlockPolicyService blockPolicyService) {
        this(notificationRepository,userRepository,eventPublisher,blockPolicyService,null);
    }
    @org.springframework.beans.factory.annotation.Autowired
    public SocialNotificationService(SocialNotificationRepository notificationRepository,
            UserRepository userRepository, ApplicationEventPublisher eventPublisher,
            BlockPolicyService blockPolicyService, MuteService muteService) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
        this.blockPolicyService = blockPolicyService;
        this.muteService = muteService;
    }

    @Transactional
    public void notifyFollow(User actor, User recipient) {
        create(actor, recipient, NotificationType.NEW_FOLLOWER, null, true);
    }

    @Transactional
    public void notifyPostInteraction(NotificationType type, User actor, Post post) {
        if (type == NotificationType.NEW_FOLLOWER || type == NotificationType.MENTION) {
            throw new IllegalArgumentException(type + " notifications must use their dedicated method");
        }
        create(actor, post.getAuthor(), type, post, type != NotificationType.POST_REPLY);
    }

    @Transactional
    public void notifyMention(User actor, User recipient, Post post) {
        create(actor, recipient, NotificationType.MENTION, post, true);
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getNotifications(
            UUID recipientId,
            boolean unreadOnly,
            Pageable pageable) {
        requireUser(recipientId);
        Pageable normalized = normalize(pageable);
        Page<SocialNotification> notifications = blockPolicyService == null
                ? (unreadOnly ? notificationRepository.findByRecipientIdAndReadAtIsNull(recipientId, normalized)
                              : notificationRepository.findByRecipientId(recipientId, normalized))
                : (unreadOnly ? notificationRepository.findVisibleUnread(recipientId, normalized)
                              : notificationRepository.findVisible(recipientId, normalized));
        return PageResponse.from(notifications, NotificationResponse::from);
    }

    @Transactional(readOnly = true)
    public UnreadNotificationCountResponse getUnreadCount(UUID recipientId) {
        requireUser(recipientId);
        return new UnreadNotificationCountResponse(blockPolicyService == null
                ? notificationRepository.countByRecipientIdAndReadAtIsNull(recipientId)
                : notificationRepository.countVisibleUnread(recipientId));
    }

    @Transactional
    public NotificationResponse markRead(UUID recipientId, UUID notificationId) {
        SocialNotification notification = notificationRepository
                .findByIdAndRecipientId(notificationId, recipientId)
                .orElseThrow(NotificationNotFoundException::new);
        notification.markRead();
        return NotificationResponse.from(notificationRepository.saveAndFlush(notification));
    }

    @Transactional
    public MarkAllNotificationsReadResponse markAllRead(UUID recipientId) {
        requireUser(recipientId);
        return new MarkAllNotificationsReadResponse(
                notificationRepository.markAllRead(recipientId, Instant.now()));
    }

    private void create(
            User actor,
            User recipient,
            NotificationType type,
            Post post,
            boolean idempotent) {
        if (actor.getId().equals(recipient.getId())) {
            return;
        }
        if (blockPolicyService != null && blockPolicyService.isBlockedEitherDirection(actor.getId(), recipient.getId())) return;
        if (muteService != null && muteService.muted(recipient.getId(), actor.getId(), null)) return;
        UUID postId = post == null ? null : post.getId();
        if (idempotent && notificationRepository.existsEquivalentEvent(
                recipient.getId(), actor.getId(), type, postId)) {
            return;
        }
        SocialNotification notification = notificationRepository.saveAndFlush(
                new SocialNotification(recipient, actor, type, post));
        eventPublisher.publishEvent(new NotificationCreatedEvent(
                notification.getId(),
                recipient.getId(),
                actor.getId(),
                type,
                postId));
    }

    private void requireUser(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException();
        }
    }

    private Pageable normalize(Pageable pageable) {
        int pageSize = Math.max(1, Math.min(pageable.getPageSize(), MAX_PAGE_SIZE));
        return PageRequest.of(pageable.getPageNumber(), pageSize, NOTIFICATION_SORT);
    }
}
