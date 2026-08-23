package com.abhiai.abhiai_backend.dto.groupchat;

import java.time.Instant;

import com.abhiai.abhiai_backend.dto.post.PostAuthorResponse;
import com.abhiai.abhiai_backend.entity.GroupParticipant;
import com.abhiai.abhiai_backend.entity.GroupRole;

public record GroupMemberResponse(
        PostAuthorResponse user,
        GroupRole role,
        Instant joinedAt) {

    public static GroupMemberResponse from(GroupParticipant participant) {
        return new GroupMemberResponse(
                PostAuthorResponse.from(participant.getUser()),
                participant.getRole(),
                participant.getJoinedAt());
    }
}
