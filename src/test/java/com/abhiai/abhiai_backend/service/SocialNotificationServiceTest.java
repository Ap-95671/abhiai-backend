package com.abhiai.abhiai_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.context.ApplicationEventPublisher;

import com.abhiai.abhiai_backend.dto.common.PageResponse;
import com.abhiai.abhiai_backend.dto.notification.MarkAllNotificationsReadResponse;
import com.abhiai.abhiai_backend.dto.notification.NotificationResponse;
import com.abhiai.abhiai_backend.dto.notification.UnreadNotificationCountResponse;
import com.abhiai.abhiai_backend.entity.NotificationType;
import com.abhiai.abhiai_backend.entity.Post;
import com.abhiai.abhiai_backend.entity.PostVisibility;
import com.abhiai.abhiai_backend.entity.SocialNotification;
import com.abhiai.abhiai_backend.entity.User;
import com.abhiai.abhiai_backend.exception.NotificationNotFoundException;
import com.abhiai.abhiai_backend.repository.SocialNotificationRepository;
import com.abhiai.abhiai_backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class SocialNotificationServiceTest {

    private static final UUID ACTOR_ID = UUID.randomUUID();
    private static final UUID RECIPIENT_ID = UUID.randomUUID();
    private static final UUID POST_ID = UUID.randomUUID();
    private static final UUID NOTIFICATION_ID = UUID.randomUUID();

    @Mock
    private SocialNotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private SocialNotificationService notificationService;
    private User actor;
    private User recipient;
    private Post post;

    @BeforeEach
    void setUp() {
        notificationService = new SocialNotificationService(
                notificationRepository, userRepository, eventPublisher);
        org.mockito.Mockito.lenient().when(notificationRepository.saveAndFlush(any(SocialNotification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        actor = user(ACTOR_ID, "actor");
        recipient = user(RECIPIENT_ID, "recipient");
        post = new Post(recipient, "Post", PostVisibility.PUBLIC);
        ReflectionTestUtils.setField(post, "id", POST_ID);
    }

    @Test
    void createsFollowAndPostInteractionNotifications() {
        notificationService.notifyFollow(actor, recipient);
        notificationService.notifyPostInteraction(NotificationType.POST_LIKE, actor, post);
        notificationService.notifyMention(actor, recipient, post);

        ArgumentCaptor<SocialNotification> captor =
                ArgumentCaptor.forClass(SocialNotification.class);
        verify(notificationRepository, times(3)).saveAndFlush(captor.capture());
        assertEquals(NotificationType.NEW_FOLLOWER, captor.getAllValues().get(0).getType());
        assertEquals(NotificationType.POST_LIKE, captor.getAllValues().get(1).getType());
        assertEquals(POST_ID, captor.getAllValues().get(1).getPost().getId());
        assertEquals(NotificationType.MENTION, captor.getAllValues().get(2).getType());
        verify(eventPublisher, times(3)).publishEvent(any(NotificationCreatedEvent.class));
    }

    @Test
    void suppressesEquivalentIdempotentEvents() {
        when(notificationRepository.existsEquivalentEvent(
                RECIPIENT_ID, ACTOR_ID, NotificationType.POST_LIKE, POST_ID))
                .thenReturn(true);

        notificationService.notifyPostInteraction(NotificationType.POST_LIKE, actor, post);

        verify(notificationRepository, never()).saveAndFlush(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void suppressesSelfNotifications() {
        notificationService.notifyFollow(actor, actor);
        Post ownPost = new Post(actor, "Own", PostVisibility.PUBLIC);
        notificationService.notifyPostInteraction(NotificationType.POST_REPLY, actor, ownPost);

        verify(notificationRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsFollowNotificationsWithAPost() {
        assertThrows(
                IllegalArgumentException.class,
                () -> notificationService.notifyPostInteraction(NotificationType.NEW_FOLLOWER, actor, post));
    }

    @Test
    void listsNotificationsWithBoundedStablePagination() {
        SocialNotification notification = notification(NotificationType.POST_REPOST);
        when(userRepository.existsById(RECIPIENT_ID)).thenReturn(true);
        when(notificationRepository.findByRecipientId(eq(RECIPIENT_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(notification), PageRequest.of(0, 100), 1));

        PageResponse<NotificationResponse> response = notificationService.getNotifications(
                RECIPIENT_ID,
                false,
                PageRequest.of(0, 500));

        assertEquals(NOTIFICATION_ID, response.content().getFirst().id());
        assertEquals(POST_ID, response.content().getFirst().postId());
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(notificationRepository).findByRecipientId(eq(RECIPIENT_ID), pageableCaptor.capture());
        assertEquals(100, pageableCaptor.getValue().getPageSize());
        assertEquals("createdAt: DESC,id: DESC", pageableCaptor.getValue().getSort().toString());
    }

    @Test
    void listsOnlyUnreadNotificationsWhenRequested() {
        SocialNotification notification = notification(NotificationType.MENTION);
        when(userRepository.existsById(RECIPIENT_ID)).thenReturn(true);
        when(notificationRepository.findByRecipientIdAndReadAtIsNull(
                eq(RECIPIENT_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(notification)));

        PageResponse<NotificationResponse> response = notificationService.getNotifications(
                RECIPIENT_ID, true, PageRequest.of(0, 20));

        assertEquals(1, response.totalElements());
        verify(notificationRepository).findByRecipientIdAndReadAtIsNull(
                eq(RECIPIENT_ID), any(Pageable.class));
    }

    @Test
    void returnsUnreadCountAndMarksAllAsRead() {
        when(userRepository.existsById(RECIPIENT_ID)).thenReturn(true);
        when(notificationRepository.countByRecipientIdAndReadAtIsNull(RECIPIENT_ID)).thenReturn(4L);
        when(notificationRepository.markAllRead(eq(RECIPIENT_ID), any(Instant.class))).thenReturn(4);

        UnreadNotificationCountResponse count = notificationService.getUnreadCount(RECIPIENT_ID);
        MarkAllNotificationsReadResponse updated = notificationService.markAllRead(RECIPIENT_ID);

        assertEquals(4, count.unreadCount());
        assertEquals(4, updated.updatedCount());
    }

    @Test
    void marksOnlyAnOwnedNotificationAsRead() {
        SocialNotification notification = notification(NotificationType.POST_LIKE);
        when(notificationRepository.findByIdAndRecipientId(NOTIFICATION_ID, RECIPIENT_ID))
                .thenReturn(Optional.of(notification));
        when(notificationRepository.saveAndFlush(notification)).thenReturn(notification);

        NotificationResponse response = notificationService.markRead(RECIPIENT_ID, NOTIFICATION_ID);

        assertNotNull(response.readAt());
        verify(notificationRepository).saveAndFlush(notification);
    }

    @Test
    void hidesAnotherUsersOrMissingNotificationAsNotFound() {
        when(notificationRepository.findByIdAndRecipientId(NOTIFICATION_ID, RECIPIENT_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                NotificationNotFoundException.class,
                () -> notificationService.markRead(RECIPIENT_ID, NOTIFICATION_ID));
    }

    private SocialNotification notification(NotificationType type) {
        SocialNotification result = new SocialNotification(recipient, actor, type, post);
        ReflectionTestUtils.setField(result, "id", NOTIFICATION_ID);
        return result;
    }

    private User user(UUID id, String username) {
        User result = new User(username, "Display", username + "@example.com", "hash");
        ReflectionTestUtils.setField(result, "id", id);
        return result;
    }
}
