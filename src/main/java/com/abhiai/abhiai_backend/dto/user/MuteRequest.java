package com.abhiai.abhiai_backend.dto.user;
import com.abhiai.abhiai_backend.entity.MuteType; import jakarta.validation.constraints.*;
public record MuteRequest(@NotNull MuteType type,String userId,@Size(max=100) String term){}
