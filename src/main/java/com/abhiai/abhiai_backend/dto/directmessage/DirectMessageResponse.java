package com.abhiai.abhiai_backend.dto.directmessage;

import java.time.Instant;
import java.util.UUID;

import com.abhiai.abhiai_backend.dto.post.PostAuthorResponse;
import com.abhiai.abhiai_backend.entity.DirectMessage;

public record DirectMessageResponse(
        UUID id,
        UUID conversationId,
        PostAuthorResponse sender,
        String content,
        boolean deleted,
        boolean readByRecipient,
        Instant createdAt,
        Instant deletedAt) {

    public static DirectMessageResponse from(DirectMessage message, boolean readByRecipient) {
        return new DirectMessageResponse(
                message.getId(),
                message.getConversation().getId(),
                PostAuthorResponse.from(message.getSender()),
                message.getContent(),
                message.isDeleted(),
                readByRecipient,
                message.getCreatedAt(),
                message.getDeletedAt());
    }
}
