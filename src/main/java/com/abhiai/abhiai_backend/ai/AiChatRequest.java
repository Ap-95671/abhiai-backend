package com.abhiai.abhiai_backend.ai;

import java.util.List;

public record AiChatRequest(List<AiChatMessage> messages, List<AiInputAttachment> attachments) {

    public AiChatRequest {
        messages = List.copyOf(messages);
        attachments = attachments == null ? List.of() : List.copyOf(attachments);
    }

    public AiChatRequest(List<AiChatMessage> messages) {
        this(messages, List.of());
    }
}
