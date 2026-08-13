package com.abhiai.abhiai_backend.ai;

import java.util.List;

public record AiChatRequest(List<AiChatMessage> messages) {

    public AiChatRequest {
        messages = List.copyOf(messages);
    }
}
