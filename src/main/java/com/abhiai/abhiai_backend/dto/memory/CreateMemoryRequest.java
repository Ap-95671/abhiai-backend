package com.abhiai.abhiai_backend.dto.memory;
import jakarta.validation.constraints.*;
import com.abhiai.abhiai_backend.entity.MemoryCategory;
public record CreateMemoryRequest(@NotBlank @Size(max=500) String content, MemoryCategory category) {
    public CreateMemoryRequest(String content) { this(content, MemoryCategory.PREFERENCE); }
}
