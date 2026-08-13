package com.abhiai.abhiai_backend.dto.chat;

import java.time.Instant;
import java.util.UUID;

import com.abhiai.abhiai_backend.entity.Message;
import com.abhiai.abhiai_backend.entity.MessageRole;

public record MessageResponse(
        UUID id,
        MessageRole role,
        String content,
        Instant createdAt) {

    public static MessageResponse from(Message message) {
        return new MessageResponse(
                message.getId(),
                message.getRole(),
                message.getContent(),
                message.getCreatedAt());
    }
}
