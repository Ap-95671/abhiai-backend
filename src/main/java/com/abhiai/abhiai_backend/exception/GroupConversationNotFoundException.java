package com.abhiai.abhiai_backend.exception;

public class GroupConversationNotFoundException extends RuntimeException {
    public GroupConversationNotFoundException() {
        super("Group conversation was not found");
    }
}
