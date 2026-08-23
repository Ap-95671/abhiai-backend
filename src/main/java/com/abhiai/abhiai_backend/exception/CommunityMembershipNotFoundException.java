package com.abhiai.abhiai_backend.exception;

public class CommunityMembershipNotFoundException extends RuntimeException {

    public CommunityMembershipNotFoundException() {
        super("Community membership not found");
    }
}
