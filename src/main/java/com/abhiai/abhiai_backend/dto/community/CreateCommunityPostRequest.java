package com.abhiai.abhiai_backend.dto.community;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCommunityPostRequest(
        @NotBlank(message = "Post text is required")
        String textContent,

        @Size(max = 4, message = "A post can contain at most 4 attachments")
        List<UUID> mediaIds) {
}
