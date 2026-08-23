package com.abhiai.abhiai_backend.dto.groupchat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InviteGroupMemberRequest(
        @NotBlank @Size(max = 30) String username) {
}
