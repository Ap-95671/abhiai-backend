package com.abhiai.abhiai_backend.dto.groupchat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.abhiai.abhiai_backend.dto.post.PostAuthorResponse;
import com.abhiai.abhiai_backend.entity.GroupRole;

public record GroupConversationResponse(
        UUID id,
        String name,
        String imageUrl,
        PostAuthorResponse owner,
        List<GroupMemberResponse> members,
        int memberCount,
        GroupRole currentUserRole,
        String lastMessagePreview,
        Instant lastMessageAt,
        Instant createdAt,
        Instant updatedAt) {
}
