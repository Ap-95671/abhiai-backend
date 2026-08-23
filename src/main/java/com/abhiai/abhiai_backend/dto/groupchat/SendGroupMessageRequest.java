package com.abhiai.abhiai_backend.dto.groupchat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendGroupMessageRequest(
        @NotBlank @Size(max = 2000) String content) {
}
