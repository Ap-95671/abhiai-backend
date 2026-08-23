package com.abhiai.abhiai_backend.dto.post;

import com.abhiai.abhiai_backend.entity.PostVisibility;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;

public record CreatePostRequest(
        @NotBlank(message = "Post text is required")
        String textContent,

        PostVisibility visibility,
        @Size(max = 4, message = "A post can contain at most 4 attachments") List<UUID> mediaIds,

        @Valid CreatePollRequest poll) {
}
