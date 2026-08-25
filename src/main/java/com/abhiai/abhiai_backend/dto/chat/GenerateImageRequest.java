package com.abhiai.abhiai_backend.dto.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GenerateImageRequest(
        @NotBlank(message = "Describe the image you want to create")
        @Size(max = 4000, message = "Image prompts may contain at most 4000 characters")
        String prompt) {
}
