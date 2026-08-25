package com.abhiai.abhiai_backend.ai;

import java.util.List;

public record AiChatRequest(
        List<AiChatMessage> messages,
        List<AiInputAttachment> attachments,
        String selectionMode,
        String selectedModelId,
        boolean fallbackAllowed,
        String providerModelId) {

    public AiChatRequest {
        messages = List.copyOf(messages);
        attachments = attachments == null ? List.of() : List.copyOf(attachments);
        selectionMode = selectionMode == null || selectionMode.isBlank() ? "AUTO" : selectionMode;
    }

    public AiChatRequest(List<AiChatMessage> messages) {
        this(messages, List.of(), "AUTO", null, true, null);
    }

    public AiChatRequest(List<AiChatMessage> messages, List<AiInputAttachment> attachments) {
        this(messages, attachments, "AUTO", null, true, null);
    }

    public AiChatRequest withProviderModelId(String modelId) {
        return new AiChatRequest(messages, attachments, selectionMode, selectedModelId, fallbackAllowed, modelId);
    }
}
