package com.abhiai.abhiai_backend.entity;

public enum GroupRole {
    OWNER,
    ADMIN,
    MEMBER;

    public boolean canManageMembers() {
        return this == OWNER || this == ADMIN;
    }
}
