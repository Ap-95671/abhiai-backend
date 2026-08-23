package com.abhiai.abhiai_backend.dto.groupchat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateGroupRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 2048) String imageUrl) {
}
