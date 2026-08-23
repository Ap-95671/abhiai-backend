package com.abhiai.abhiai_backend.exception;

public class PostNotFoundException extends RuntimeException {

    public PostNotFoundException() {
        super("Post not found");
    }
}
