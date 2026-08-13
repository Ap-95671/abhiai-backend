package com.abhiai.abhiai_backend.dto.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RenameConversationRequest(

        @NotBlank(message = "Title cannot be empty")
        @Size(max = 255, message = "Title cannot exceed 255 characters")
        String title

) {
}