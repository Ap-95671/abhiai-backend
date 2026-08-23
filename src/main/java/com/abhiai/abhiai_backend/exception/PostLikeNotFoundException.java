package com.abhiai.abhiai_backend.exception;

public class PostLikeNotFoundException extends RuntimeException {

    public PostLikeNotFoundException() {
        super("Post like not found");
    }
}
