package com.abhiai.abhiai_backend.dto.memory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateMemoryRequest(@NotBlank @Size(max = 500) String content) {
}
