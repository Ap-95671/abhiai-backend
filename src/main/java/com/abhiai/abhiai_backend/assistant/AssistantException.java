package com.abhiai.abhiai_backend.assistant;

import org.springframework.http.HttpStatus;

public class AssistantException extends RuntimeException {
    private final HttpStatus status;
    public AssistantException(HttpStatus status, String safeMessage) {
        super(safeMessage);
        this.status = status;
    }
    public HttpStatus status() { return status; }
}
