package com.abhiai.abhiai_backend.exception;

public class GroupMessageNotFoundException extends RuntimeException {
    public GroupMessageNotFoundException() {
        super("Group message was not found");
    }
}
