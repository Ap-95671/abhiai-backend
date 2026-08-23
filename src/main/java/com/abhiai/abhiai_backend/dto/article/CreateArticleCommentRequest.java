package com.abhiai.abhiai_backend.dto.article;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateArticleCommentRequest(@NotBlank @Size(max=2000) String content) {}
