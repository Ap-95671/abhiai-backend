package com.abhiai.abhiai_backend.dto.story;

import java.util.UUID;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateStoryRequest(
        @Size(max = 500, message = "Story text must not exceed 500 characters") String textContent,
        UUID mediaId,
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Background color must be a six-digit hex color")
        String backgroundColor) {
}
