package com.abhiai.abhiai_backend.ai;

import java.util.List;

public record AiChatRequest(
        List<AiChatMessage> messages,
        List<AiInputAttachment> attachments,
        String selectionMode,
        String selectedModelId,
        boolean fallbackAllowed,
        String providerModelId,
        String originalMessage, ExecutionContext executionContext) {

    public record ExecutionContext(String requestId, java.util.UUID conversationId, List<String> toolCalls) {
        public ExecutionContext { toolCalls = List.copyOf(toolCalls); }
    }
    public AiChatRequest(List<AiChatMessage> messages, List<AiInputAttachment> attachments, String selectionMode,
                         String selectedModelId, boolean fallbackAllowed, String providerModelId, String originalMessage) {
        this(messages, attachments, selectionMode, selectedModelId, fallbackAllowed, providerModelId, originalMessage, null);
    }
    public AiChatRequest {
        if (executionContext == null) executionContext = new ExecutionContext(
                com.abhiai.abhiai_backend.config.RequestCorrelationFilter.currentId(), null, List.of());
        messages = List.copyOf(messages);
        originalMessage = originalMessage == null ? messages.stream()
                .filter(m -> m.role() == com.abhiai.abhiai_backend.entity.MessageRole.USER)
                .reduce((a, b) -> b).map(AiChatMessage::content).orElse("") : originalMessage;
        selectionMode = selectionMode == null ? "AUTO" : selectionMode.trim().toUpperCase(java.util.Locale.ROOT);
        attachments = attachments == null ? List.of() : List.copyOf(attachments);
        selectionMode = selectionMode == null || selectionMode.isBlank() ? "AUTO" : selectionMode;
    }

    public AiChatRequest(List<AiChatMessage> messages, List<AiInputAttachment> attachments, String selectionMode,
                         String selectedModelId, boolean fallbackAllowed, String providerModelId) {
        this(messages, attachments, selectionMode, selectedModelId, fallbackAllowed, providerModelId, null);
    }

    public AiChatRequest withOriginalMessage(String text) {
        return new AiChatRequest(messages, attachments, selectionMode, selectedModelId, fallbackAllowed, providerModelId, text, executionContext);
    }

    public AiChatRequest withMessages(List<AiChatMessage> context) {
        return new AiChatRequest(context, attachments, selectionMode, selectedModelId, fallbackAllowed, providerModelId, originalMessage, executionContext);
    }

    public AiChatRequest(List<AiChatMessage> messages) {
        this(messages, List.of(), "AUTO", null, true, null);
    }

    public AiChatRequest(List<AiChatMessage> messages, List<AiInputAttachment> attachments) {
        this(messages, attachments, "AUTO", null, true, null);
    }

    public AiChatRequest withExecutionContext(java.util.UUID conversationId, List<String> toolCalls) {
        return new AiChatRequest(messages, attachments, selectionMode, selectedModelId, fallbackAllowed, providerModelId, originalMessage,
                new ExecutionContext(executionContext.requestId(), conversationId, toolCalls));
    }

    public AiChatRequest withProviderModelId(String modelId) {
        return new AiChatRequest(messages, attachments, selectionMode, selectedModelId, fallbackAllowed, modelId, originalMessage, executionContext);
    }
}
