package com.abhiai.abhiai_backend.dto.repost;

import java.time.Instant;
import java.util.UUID;

import com.abhiai.abhiai_backend.entity.PostRepost;
import com.abhiai.abhiai_backend.entity.User;
import com.abhiai.abhiai_backend.entity.VerifiedStatus;

public record PostRepostUserResponse(
        UUID id,
        String username,
        String displayName,
        String profilePicture,
        VerifiedStatus verifiedStatus,
        Instant repostedAt) {

    public static PostRepostUserResponse from(PostRepost repost) {
        User user = repost.getUser();
        return new PostRepostUserResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getProfilePicture(),
                user.getVerifiedStatus(),
                repost.getCreatedAt());
    }
}
