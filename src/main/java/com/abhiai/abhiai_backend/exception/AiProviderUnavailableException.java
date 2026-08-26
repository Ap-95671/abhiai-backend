package com.abhiai.abhiai_backend.exception;

public class AiProviderUnavailableException extends AiProviderException {

    public AiProviderUnavailableException() {
        this("The AI provider is not configured");
    }

    public AiProviderUnavailableException(String message) {
        super(message, AiProviderFailureKind.CONFIGURATION);
    }
}
