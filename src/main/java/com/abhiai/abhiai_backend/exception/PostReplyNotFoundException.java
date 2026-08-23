package com.abhiai.abhiai_backend.exception;

public class PostReplyNotFoundException extends RuntimeException {

    public PostReplyNotFoundException() {
        super("Post reply not found");
    }
}
