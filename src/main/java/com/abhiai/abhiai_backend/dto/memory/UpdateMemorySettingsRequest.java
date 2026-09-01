package com.abhiai.abhiai_backend.dto.memory;

import jakarta.validation.constraints.NotNull;

public record UpdateMemorySettingsRequest(@NotNull Boolean enabled) {
}
