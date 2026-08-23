package com.abhiai.abhiai_backend.exception;

public class DuplicateFollowException extends RuntimeException {

    public DuplicateFollowException() {
        super("You already follow this user");
    }
}
