package com.abhiai.abhiai_backend.dto.notification;

import java.time.Instant;
import java.util.UUID;

import com.abhiai.abhiai_backend.dto.post.PostAuthorResponse;
import com.abhiai.abhiai_backend.entity.SocialNotification;
import com.abhiai.abhiai_backend.entity.NotificationType;

public record NotificationResponse(
        UUID id,
        NotificationType type,
        PostAuthorResponse actor,
        String entityType,
        UUID entityId,
        UUID postId,
        boolean read,
        Instant readAt,
        Instant createdAt) {

    public static NotificationResponse from(SocialNotification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                PostAuthorResponse.from(notification.getActor()),
                notification.getPost() == null ? "USER" : "POST",
                notification.getPost() == null
                        ? notification.getActor().getId()
                        : notification.getPost().getId(),
                notification.getPost() == null ? null : notification.getPost().getId(),
                notification.getReadAt() != null,
                notification.getReadAt(),
                notification.getCreatedAt());
    }
}
