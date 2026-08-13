package com.abhiai.abhiai_backend.dto.chat;

import java.time.Instant;
import java.util.UUID;

import com.abhiai.abhiai_backend.entity.Conversation;

public record ConversationSummaryResponse(
        UUID id,
        String title,
        Instant createdAt,
        Instant updatedAt) {

    public static ConversationSummaryResponse from(Conversation conversation) {
        return new ConversationSummaryResponse(
                conversation.getId(),
                conversation.getTitle(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt());
    }
}
