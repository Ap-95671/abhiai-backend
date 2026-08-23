package com.abhiai.abhiai_backend.dto.directmessage;

import java.time.Instant;
import java.util.UUID;

import com.abhiai.abhiai_backend.dto.post.PostAuthorResponse;

public record DirectConversationResponse(
        UUID id,
        PostAuthorResponse participant,
        String lastMessagePreview,
        Instant lastMessageAt,
        long unreadCount,
        Instant createdAt,
        Instant updatedAt) {
}
