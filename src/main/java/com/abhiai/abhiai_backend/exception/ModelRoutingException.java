package com.abhiai.abhiai_backend.exception;

public class ModelRoutingException extends RuntimeException {
    private final String code;

    public ModelRoutingException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() { return code; }
}
