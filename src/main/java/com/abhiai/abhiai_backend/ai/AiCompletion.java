package com.abhiai.abhiai_backend.ai;

public record AiCompletion(
        String content,
        String provider,
        String model,
        String finishReason,
        Integer inputTokens,
        Integer outputTokens,
        long latencyMs,
        boolean fallbackUsed) {

    public AiCompletion(String content) {
        this(content, null, null, null, null, null, 0, false);
    }

    public AiCompletion attributed(String provider, String model, long latencyMs, boolean fallbackUsed) {
        return new AiCompletion(content, provider, model, finishReason, inputTokens, outputTokens, latencyMs, fallbackUsed);
    }
}
