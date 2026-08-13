package com.abhiai.abhiai_backend.dto.chat;

public record ChatExchangeResponse(
        MessageResponse userMessage,
        MessageResponse assistantMessage) {
}
