package com.abhiai.abhiai_backend.dto.memory;

import java.util.List;

public record MemorySettingsResponse(boolean enabled, List<UserMemoryResponse> memories) {
    public MemorySettingsResponse {
        memories = List.copyOf(memories);
    }
}
