package com.abhiai.abhiai_backend.dto.article;
import java.util.UUID;
public record ArticleShareResponse(UUID articleId, long shareCount) {}
