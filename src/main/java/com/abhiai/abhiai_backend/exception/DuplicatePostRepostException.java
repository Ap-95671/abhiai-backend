package com.abhiai.abhiai_backend.exception;

public class DuplicatePostRepostException extends RuntimeException {

    public DuplicatePostRepostException() {
        super("You have already reposted this post");
    }
}
