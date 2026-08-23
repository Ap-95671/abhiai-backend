package com.abhiai.abhiai_backend.exception;

public class PostRepostNotFoundException extends RuntimeException {

    public PostRepostNotFoundException() {
        super("Post repost not found");
    }
}
