package com.abhiai.abhiai_backend.dto.chat;

import java.time.Instant;
import java.util.UUID;
import java.util.List;

import com.abhiai.abhiai_backend.entity.Message;
import com.abhiai.abhiai_backend.entity.MessageRole;

public record MessageResponse(
        UUID id,
        MessageRole role,
        String content,
        Instant createdAt,
        List<ConversationAttachmentResponse> attachments) {

    public static MessageResponse from(Message message) {
        return new MessageResponse(
                message.getId(),
                message.getRole(),
                message.getContent(),
                message.getCreatedAt(),
                List.of());
    }

    public static MessageResponse from(Message message, List<ConversationAttachmentResponse> attachments) {
        return new MessageResponse(
                message.getId(),
                message.getRole(),
                message.getContent(),
                message.getCreatedAt(),
                List.copyOf(attachments));
    }
}
