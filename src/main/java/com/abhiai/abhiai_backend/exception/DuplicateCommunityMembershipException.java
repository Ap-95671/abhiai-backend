package com.abhiai.abhiai_backend.exception;

public class DuplicateCommunityMembershipException extends RuntimeException {

    public DuplicateCommunityMembershipException() {
        super("You are already a member of this community");
    }
}
