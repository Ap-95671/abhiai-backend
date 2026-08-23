package com.abhiai.abhiai_backend.exception;

public class DirectMessageNotFoundException extends RuntimeException {
    public DirectMessageNotFoundException() {
        super("Direct message was not found");
    }
}
