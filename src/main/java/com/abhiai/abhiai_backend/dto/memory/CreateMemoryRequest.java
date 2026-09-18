package com.abhiai.abhiai_backend.dto.memory;
import jakarta.validation.constraints.*;
import com.abhiai.abhiai_backend.entity.*;
public record CreateMemoryRequest(@NotBlank @Size(max=500) String content, MemoryCategory category,
    MemoryScope scope,@Size(max=128) String scopeKey,@Size(max=80) String preferenceKey) {
    public CreateMemoryRequest(String content){this(content,MemoryCategory.PREFERENCE,null,null,null);}
    public CreateMemoryRequest(String content,MemoryCategory category){this(content,category,null,null,null);}
}
