package com.abhiai.abhiai_backend.exception;

public class PostBookmarkNotFoundException extends RuntimeException {

    public PostBookmarkNotFoundException() {
        super("Post bookmark not found");
    }
}
