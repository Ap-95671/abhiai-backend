package com.abhiai.abhiai_backend.exception;

public class ConversationNotFoundException extends RuntimeException {

    public ConversationNotFoundException() {
        super("Conversation was not found");
    }
}
