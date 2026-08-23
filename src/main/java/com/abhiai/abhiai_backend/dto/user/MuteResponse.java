package com.abhiai.abhiai_backend.dto.user;
import java.time.Instant; import java.util.UUID; import com.abhiai.abhiai_backend.entity.*;
public record MuteResponse(UUID id,MuteType type,UUID userId,String username,String term,Instant createdAt){
 public static MuteResponse from(UserMute m){var u=m.getMutedUser();return new MuteResponse(m.getId(),m.getType(),u==null?null:u.getId(),u==null?null:u.getUsername(),m.getMutedTerm(),m.getCreatedAt());}
}
