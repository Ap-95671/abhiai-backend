package com.abhiai.abhiai_backend.dto.chat;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendMessageRequest(
        @NotBlank(message = "Message content is required")
        @Size(max = 10000, message = "Message content must not exceed 10000 characters")
        String content,

        @Size(max = 5, message = "A message may contain at most 5 attachments")
        List<UUID> attachmentIds,

        boolean externalProcessingAllowed,

        boolean webSearchAllowed) {

    public SendMessageRequest(String content) {
        this(content, List.of(), false, false);
    }
}
