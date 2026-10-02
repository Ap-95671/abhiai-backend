package com.abhiai.abhiai_backend.exception;

public enum AiProviderFailureKind {
    UNKNOWN, AUTHENTICATION, AUTHORIZATION, BILLING, RATE_LIMIT, MODEL_UNAVAILABLE,
    UPSTREAM_UNAVAILABLE, CONFIGURATION, TIMEOUT, INVALID_REQUEST, CONTENT_RESTRICTION, TOOL_FAILURE, SEARCH_FAILURE;

    public static AiProviderFailureKind httpStatus(int status) {
        return switch (status) {
            case 400, 413, 422 -> INVALID_REQUEST;
            case 401 -> AUTHENTICATION;
            case 402 -> BILLING;
            case 403 -> AUTHORIZATION;
            case 404 -> MODEL_UNAVAILABLE;
            case 408, 504 -> TIMEOUT;
            case 429 -> RATE_LIMIT;
            default -> status >= 500 ? UPSTREAM_UNAVAILABLE : UNKNOWN;
        };
    }

    public static AiProviderFailureKind classify(Throwable failure) {
        if (failure instanceof AiProviderException provider && provider.kind() != UNKNOWN) return provider.kind();
        for (Throwable cause = failure; cause != null; cause = cause.getCause())
            if (cause instanceof java.net.http.HttpTimeoutException || cause instanceof java.util.concurrent.TimeoutException)
                return TIMEOUT;
        return UNKNOWN;
    }
}
