package com.abhiai.abhiai_backend.dto.directmessage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateDirectConversationRequest(
        @NotBlank(message = "Recipient username is required")
        @Size(max = 31, message = "Recipient username must not exceed 31 characters")
        String recipientUsername) {
}
