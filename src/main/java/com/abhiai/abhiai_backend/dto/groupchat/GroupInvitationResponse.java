package com.abhiai.abhiai_backend.dto.groupchat;

import java.time.Instant;
import java.util.UUID;

import com.abhiai.abhiai_backend.dto.post.PostAuthorResponse;
import com.abhiai.abhiai_backend.entity.GroupInvitation;
import com.abhiai.abhiai_backend.entity.GroupInvitationStatus;

public record GroupInvitationResponse(
        UUID id,
        UUID groupId,
        String groupName,
        String groupImageUrl,
        PostAuthorResponse inviter,
        GroupInvitationStatus status,
        Instant createdAt,
        Instant respondedAt) {

    public static GroupInvitationResponse from(GroupInvitation invitation) {
        return new GroupInvitationResponse(
                invitation.getId(),
                invitation.getConversation().getId(),
                invitation.getConversation().getName(),
                invitation.getConversation().getImageUrl(),
                PostAuthorResponse.from(invitation.getInviter()),
                invitation.getStatus(),
                invitation.getCreatedAt(),
                invitation.getRespondedAt());
    }
}
