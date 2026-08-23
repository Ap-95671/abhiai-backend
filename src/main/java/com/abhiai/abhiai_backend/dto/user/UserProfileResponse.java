package com.abhiai.abhiai_backend.dto.user;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.abhiai.abhiai_backend.entity.User;
import com.abhiai.abhiai_backend.entity.VerifiedStatus;
import com.abhiai.abhiai_backend.entity.AccountPrivacy;

public record UserProfileResponse(
        UUID id,
        String username,
        String displayName,
        String bio,
        String profilePicture,
        String coverPicture,
        UUID profileMediaId,
        UUID coverMediaId,
        String location,
        String website,
        LocalDate dateOfBirth,
        Instant createdAt,
        Instant updatedAt,
        VerifiedStatus verifiedStatus,
        long followerCount,
        long followingCount,
        long postCount,
        boolean showLikesOnProfile,
        AccountPrivacy accountPrivacy) {

    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getBio(),
                user.getProfilePicture(),
                user.getCoverPicture(),
                user.getProfileMedia() == null ? null : user.getProfileMedia().getId(),
                user.getCoverMedia() == null ? null : user.getCoverMedia().getId(),
                user.getLocation(),
                user.getWebsite(),
                user.getDateOfBirth(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getVerifiedStatus(),
                user.getFollowerCount(),
                user.getFollowingCount(),
                user.getPostCount(),
                user.isShowLikesOnProfile(),user.getAccountPrivacy());
    }
}
