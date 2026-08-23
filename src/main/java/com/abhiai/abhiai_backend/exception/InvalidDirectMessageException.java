package com.abhiai.abhiai_backend.exception;

public class InvalidDirectMessageException extends RuntimeException {
    public InvalidDirectMessageException(String message) {
        super(message);
    }
}
