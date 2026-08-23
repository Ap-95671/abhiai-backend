package com.abhiai.abhiai_backend.dto.user;

import java.time.Instant;
import java.util.UUID;

import com.abhiai.abhiai_backend.dto.post.PostAuthorResponse;
import com.abhiai.abhiai_backend.dto.post.PostResponse;
import com.abhiai.abhiai_backend.entity.PostReply;

public record ProfileReplyResponse(
        UUID id,
        PostAuthorResponse author,
        String textContent,
        Instant createdAt,
        Instant updatedAt,
        PostResponse post) {

    public static ProfileReplyResponse from(PostReply reply) {
        return new ProfileReplyResponse(
                reply.getId(),
                PostAuthorResponse.from(reply.getAuthor()),
                reply.getTextContent(),
                reply.getCreatedAt(),
                reply.getUpdatedAt(),
                PostResponse.from(reply.getPost()));
    }
}
