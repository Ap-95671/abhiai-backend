package com.abhiai.abhiai_backend.dto.chat;

import java.time.Instant;
import java.util.UUID;
import java.util.List;

import com.abhiai.abhiai_backend.entity.Message;
import com.abhiai.abhiai_backend.entity.MessageRole;

public record MessageResponse(
        UUID id,
        MessageRole role,
        String content,
        Instant createdAt,
        List<ConversationAttachmentResponse> attachments,
        String provider,
        String model,
        Integer inputTokens,
        Integer outputTokens,
        Long latencyMs,
        boolean fallbackUsed) {

    public static MessageResponse from(Message message) {
        return new MessageResponse(
                message.getId(),
                message.getRole(),
                message.getContent(),
                message.getCreatedAt(),
                List.of(), message.getAiProvider(), message.getAiModel(), message.getInputTokens(),
                message.getOutputTokens(), message.getLatencyMs(), Boolean.TRUE.equals(message.getFallbackUsed()));
    }

    public static MessageResponse from(Message message, List<ConversationAttachmentResponse> attachments) {
        return new MessageResponse(
                message.getId(),
                message.getRole(),
                message.getContent(),
                message.getCreatedAt(),
                List.copyOf(attachments), message.getAiProvider(), message.getAiModel(), message.getInputTokens(),
                message.getOutputTokens(), message.getLatencyMs(), Boolean.TRUE.equals(message.getFallbackUsed()));
    }
}
