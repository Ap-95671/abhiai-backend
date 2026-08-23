package com.abhiai.abhiai_backend.exception;

public class DirectConversationNotFoundException extends RuntimeException {
    public DirectConversationNotFoundException() {
        super("Direct conversation was not found");
    }
}
