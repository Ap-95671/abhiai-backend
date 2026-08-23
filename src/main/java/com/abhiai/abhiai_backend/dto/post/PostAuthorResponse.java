package com.abhiai.abhiai_backend.dto.post;

import java.util.UUID;

import com.abhiai.abhiai_backend.entity.User;
import com.abhiai.abhiai_backend.entity.VerifiedStatus;

public record PostAuthorResponse(
        UUID id,
        String username,
        String displayName,
        String profilePicture,
        UUID profileMediaId,
        VerifiedStatus verifiedStatus) {

    public static PostAuthorResponse from(User user) {
        return new PostAuthorResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getProfilePicture(),
                user.getProfileMedia() == null ? null : user.getProfileMedia().getId(),
                user.getVerifiedStatus());
    }
}
