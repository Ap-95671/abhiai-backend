package com.abhiai.abhiai_backend.dto.reply;

import java.time.Instant;
import java.util.UUID;

import com.abhiai.abhiai_backend.dto.post.PostAuthorResponse;
import com.abhiai.abhiai_backend.entity.PostReply;

public record PostReplyResponse(
        UUID id,
        UUID postId,
        PostAuthorResponse author,
        String textContent,
        Instant createdAt,
        Instant updatedAt) {

    public static PostReplyResponse from(PostReply reply) {
        return new PostReplyResponse(
                reply.getId(),
                reply.getPost().getId(),
                PostAuthorResponse.from(reply.getAuthor()),
                reply.getTextContent(),
                reply.getCreatedAt(),
                reply.getUpdatedAt());
    }
}
