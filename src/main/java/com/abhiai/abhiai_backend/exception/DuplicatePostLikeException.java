package com.abhiai.abhiai_backend.exception;

public class DuplicatePostLikeException extends RuntimeException {

    public DuplicatePostLikeException() {
        super("You have already liked this post");
    }
}
