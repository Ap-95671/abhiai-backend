package com.abhiai.abhiai_backend.dto.mention;

import java.util.UUID;

import com.abhiai.abhiai_backend.entity.User;
import com.abhiai.abhiai_backend.entity.VerifiedStatus;

public record MentionResponse(
        UUID userId,
        String username,
        String displayName,
        String profilePicture,
        UUID profileMediaId,
        VerifiedStatus verifiedStatus) {

    public static MentionResponse from(User user) {
        return new MentionResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getProfilePicture(),
                user.getProfileMedia() == null ? null : user.getProfileMedia().getId(),
                user.getVerifiedStatus());
    }
}
