package com.abhiai.abhiai_backend.dto.story;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StoryReactionRequest(
        @NotBlank(message = "Choose a reaction")
        @Size(max = 16, message = "Reaction is too long")
        String reaction) {
}
