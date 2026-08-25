package com.abhiai.abhiai_backend.dto.chat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.abhiai.abhiai_backend.entity.Conversation;

public record ConversationDetailResponse(
        UUID id,
        String title,
        Instant createdAt,
        Instant updatedAt,
        String modelSelectionMode,
        String preferredModelId,
        List<MessageResponse> messages) {

    public static ConversationDetailResponse from(Conversation conversation, List<MessageResponse> messages) {
        return new ConversationDetailResponse(
                conversation.getId(),
                conversation.getTitle(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt(),
                conversation.getModelSelectionMode().name(),
                conversation.getPreferredModelId(),
                messages);
    }
}
