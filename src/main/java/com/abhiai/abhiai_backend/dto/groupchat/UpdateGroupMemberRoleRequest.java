package com.abhiai.abhiai_backend.dto.groupchat;

import com.abhiai.abhiai_backend.entity.GroupRole;

import jakarta.validation.constraints.NotNull;

public record UpdateGroupMemberRoleRequest(@NotNull GroupRole role) {
}
