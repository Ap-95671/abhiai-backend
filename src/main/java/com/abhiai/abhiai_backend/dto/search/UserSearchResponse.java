package com.abhiai.abhiai_backend.dto.search;

import java.util.UUID;

import com.abhiai.abhiai_backend.entity.User;
import com.abhiai.abhiai_backend.entity.VerifiedStatus;

public record UserSearchResponse(
        UUID id,
        String username,
        String displayName,
        String bio,
        String profilePicture,
        UUID profileMediaId,
        VerifiedStatus verifiedStatus,
        long followerCount) {

    public static UserSearchResponse from(User user) {
        return new UserSearchResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getBio(),
                user.getProfilePicture(),
                user.getProfileMedia() == null ? null : user.getProfileMedia().getId(),
                user.getVerifiedStatus(),
                user.getFollowerCount());
    }
}
