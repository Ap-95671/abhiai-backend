package com.abhiai.abhiai_backend.exception;

public enum AiProviderFailureKind {
    UNKNOWN,
    AUTHENTICATION,
    AUTHORIZATION,
    BILLING,
    RATE_LIMIT,
    MODEL_UNAVAILABLE,
    UPSTREAM_UNAVAILABLE,
    CONFIGURATION
}
