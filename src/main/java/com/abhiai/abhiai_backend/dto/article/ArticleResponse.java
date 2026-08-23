package com.abhiai.abhiai_backend.dto.article;

import java.time.Instant;
import java.util.UUID;
import com.abhiai.abhiai_backend.dto.post.PostAuthorResponse;
import com.abhiai.abhiai_backend.entity.Article;

public record ArticleResponse(UUID id, PostAuthorResponse author, String title, String summary, String coverImageUrl,
        String content, long likeCount, long commentCount, long shareCount, boolean likedByCurrentUser,
        Instant publishedAt, Instant updatedAt) {
    public static ArticleResponse from(Article article, boolean liked){return new ArticleResponse(article.getId(),PostAuthorResponse.from(article.getAuthor()),article.getTitle(),article.getSummary(),article.getCoverImageUrl(),article.getContent(),article.getLikeCount(),article.getCommentCount(),article.getShareCount(),liked,article.getPublishedAt(),article.getUpdatedAt());}
}
