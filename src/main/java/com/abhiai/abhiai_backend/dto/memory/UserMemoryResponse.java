package com.abhiai.abhiai_backend.dto.memory;

import java.time.Instant;
import java.util.UUID;

import com.abhiai.abhiai_backend.entity.UserMemory;

public record UserMemoryResponse(UUID id, String content, Instant createdAt, Instant updatedAt, com.abhiai.abhiai_backend.entity.MemoryCategory category, String source, com.abhiai.abhiai_backend.entity.MemoryScope scope, String scopeKey, String preferenceKey) {
    public static UserMemoryResponse from(UserMemory memory) {
        return new UserMemoryResponse(memory.getId(), memory.getContent(), memory.getCreatedAt(), memory.getUpdatedAt(), memory.getCategory(), memory.getSource(),memory.getScope(),memory.getScopeKey(),memory.getPreferenceKey());
    }
}
