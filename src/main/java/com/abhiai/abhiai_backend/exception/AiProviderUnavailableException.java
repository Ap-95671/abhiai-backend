package com.abhiai.abhiai_backend.exception;

public class AiProviderUnavailableException extends AiProviderException {

    public AiProviderUnavailableException() {
        super("The AI provider is not configured");
    }
}
