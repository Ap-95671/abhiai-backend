package com.abhiai.abhiai_backend.exception;

public class FollowRelationshipNotFoundException extends RuntimeException {

    public FollowRelationshipNotFoundException() {
        super("You do not follow this user");
    }
}
