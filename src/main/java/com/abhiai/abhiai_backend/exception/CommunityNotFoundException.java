package com.abhiai.abhiai_backend.exception;

public class CommunityNotFoundException extends RuntimeException {

    public CommunityNotFoundException() {
        super("Community not found");
    }
}
