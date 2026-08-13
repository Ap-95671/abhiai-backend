package com.abhiai.abhiai_backend.ai;

import com.abhiai.abhiai_backend.entity.MessageRole;

public record AiChatMessage(MessageRole role, String content) {
}
