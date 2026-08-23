package com.abhiai.abhiai_backend.dto.reply;

import jakarta.validation.constraints.NotBlank;

public record CreateReplyRequest(
        @NotBlank(message = "Reply text is required")
        String textContent) {
}
