package com.abhiai.abhiai_backend.dto.groupchat;

import java.time.Instant;
import java.util.UUID;

import com.abhiai.abhiai_backend.dto.post.PostAuthorResponse;
import com.abhiai.abhiai_backend.entity.GroupMessage;

public record GroupMessageResponse(
        UUID id,
        UUID conversationId,
        PostAuthorResponse sender,
        String content,
        boolean deleted,
        Instant createdAt,
        Instant deletedAt) {

    public static GroupMessageResponse from(GroupMessage message) {
        return new GroupMessageResponse(
                message.getId(),
                message.getConversation().getId(),
                PostAuthorResponse.from(message.getSender()),
                message.getContent(),
                message.isDeleted(),
                message.getCreatedAt(),
                message.getDeletedAt());
    }
}
