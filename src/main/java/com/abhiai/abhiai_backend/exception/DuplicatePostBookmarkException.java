package com.abhiai.abhiai_backend.exception;

public class DuplicatePostBookmarkException extends RuntimeException {

    public DuplicatePostBookmarkException() {
        super("You have already bookmarked this post");
    }
}
