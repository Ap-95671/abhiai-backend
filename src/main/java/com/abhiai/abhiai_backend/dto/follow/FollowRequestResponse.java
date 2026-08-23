package com.abhiai.abhiai_backend.dto.follow;
import java.time.Instant;import java.util.UUID;import com.abhiai.abhiai_backend.entity.*;
public record FollowRequestResponse(UUID id,UUID requesterId,String username,String displayName,FollowRequestStatus status,Instant createdAt){public static FollowRequestResponse from(FollowRequest r){var u=r.getRequester();return new FollowRequestResponse(r.getId(),u.getId(),u.getUsername(),u.getDisplayName(),r.getStatus(),r.getCreatedAt());}}
