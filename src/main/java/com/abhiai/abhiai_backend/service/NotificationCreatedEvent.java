package com.abhiai.abhiai_backend.service;

import java.util.UUID;

import com.abhiai.abhiai_backend.entity.NotificationType;

public record NotificationCreatedEvent(
        UUID notificationId,
        UUID recipientId,
        UUID actorId,
        NotificationType type,
        UUID postId) {
}
