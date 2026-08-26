package com.abhiai.abhiai_backend.exception;

public class AiProviderException extends RuntimeException {

    private final AiProviderFailureKind kind;

    public AiProviderException(String message) {
        this(message, AiProviderFailureKind.UNKNOWN, null);
    }

    public AiProviderException(String message, Throwable cause) {
        this(message, AiProviderFailureKind.UNKNOWN, cause);
    }

    public AiProviderException(String message, AiProviderFailureKind kind) {
        this(message, kind, null);
    }

    public AiProviderException(String message, AiProviderFailureKind kind, Throwable cause) {
        super(message, cause);
        this.kind = kind == null ? AiProviderFailureKind.UNKNOWN : kind;
    }

    public AiProviderFailureKind kind() { return kind; }
}
