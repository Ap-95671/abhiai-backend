package com.abhiai.abhiai_backend.ai;

public record AiCompletion(
        String content,
        String provider,
        String model,
        String finishReason,
        Integer inputTokens,
        Integer outputTokens,
        long latencyMs,
        boolean fallbackUsed, com.abhiai.abhiai_backend.ai.pipeline.AiExecutionMetadata execution) {

    public AiCompletion(String content, String provider, String model, String finishReason, Integer inputTokens,
                        Integer outputTokens, long latencyMs, boolean fallbackUsed) {
        this(content, provider, model, finishReason, inputTokens, outputTokens, latencyMs, fallbackUsed, null);
    }
    public AiCompletion withExecution(com.abhiai.abhiai_backend.ai.pipeline.AiExecutionMetadata metadata) {
        return new AiCompletion(content, provider, model, finishReason, inputTokens, outputTokens, latencyMs, fallbackUsed, metadata);
    }

    public AiCompletion(String content) {
        this(content, null, null, null, null, null, 0, false);
    }

    public AiCompletion attributed(String provider, String model, long latencyMs, boolean fallbackUsed) {
        return new AiCompletion(content, provider, model, finishReason, inputTokens, outputTokens, latencyMs, fallbackUsed, execution);
    }
}
