package com.abhiai.abhiai_backend.exception;

public class GroupInvitationNotFoundException extends RuntimeException {
    public GroupInvitationNotFoundException() {
        super("Group invitation was not found");
    }
}
