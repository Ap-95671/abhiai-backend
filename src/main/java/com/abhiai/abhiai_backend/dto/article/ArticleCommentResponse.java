package com.abhiai.abhiai_backend.dto.article;

import java.time.Instant;
import java.util.UUID;
import com.abhiai.abhiai_backend.dto.post.PostAuthorResponse;
import com.abhiai.abhiai_backend.entity.ArticleComment;

public record ArticleCommentResponse(UUID id, PostAuthorResponse author, String content, Instant createdAt, Instant updatedAt) {
    public static ArticleCommentResponse from(ArticleComment comment){return new ArticleCommentResponse(comment.getId(),PostAuthorResponse.from(comment.getAuthor()),comment.getContent(),comment.getCreatedAt(),comment.getUpdatedAt());}
}
