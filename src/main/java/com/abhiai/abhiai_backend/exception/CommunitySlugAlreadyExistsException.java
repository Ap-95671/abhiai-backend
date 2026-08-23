package com.abhiai.abhiai_backend.exception;

public class CommunitySlugAlreadyExistsException extends RuntimeException {

    public CommunitySlugAlreadyExistsException() {
        super("A community with this slug already exists");
    }
}
