package com.abhiai.abhiai_backend.dto.chat;

import jakarta.validation.constraints.Size;

public record CreateConversationRequest(

        @Size(max = 255, message = "Conversation title must not exceed 255 characters")
        String title

) {
}