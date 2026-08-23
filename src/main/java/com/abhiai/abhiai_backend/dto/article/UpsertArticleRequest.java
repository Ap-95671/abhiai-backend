package com.abhiai.abhiai_backend.dto.article;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpsertArticleRequest(
        @NotBlank @Size(min=3,max=180) String title,
        @NotBlank @Size(max=320) String summary,
        @Size(max=2048) String coverImageUrl,
        @NotBlank @Size(min=50,max=50000) String content) {}
