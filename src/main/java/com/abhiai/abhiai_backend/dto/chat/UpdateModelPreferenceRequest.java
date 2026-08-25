package com.abhiai.abhiai_backend.dto.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateModelPreferenceRequest(
        @NotBlank String selectionMode,
        @Size(max = 160) String selectedModelId) {
}
