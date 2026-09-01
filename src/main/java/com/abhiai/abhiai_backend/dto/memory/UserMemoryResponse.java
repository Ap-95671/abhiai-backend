package com.abhiai.abhiai_backend.dto.memory;

import java.time.Instant;
import java.util.UUID;

import com.abhiai.abhiai_backend.entity.UserMemory;

public record UserMemoryResponse(UUID id, String content, Instant createdAt, Instant updatedAt) {
    public static UserMemoryResponse from(UserMemory memory) {
        return new UserMemoryResponse(memory.getId(), memory.getContent(), memory.getCreatedAt(), memory.getUpdatedAt());
    }
}
