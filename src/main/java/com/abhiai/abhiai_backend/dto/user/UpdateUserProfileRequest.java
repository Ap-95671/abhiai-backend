package com.abhiai.abhiai_backend.dto.user;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserProfileRequest(
        @Size(min = 3, max = 30, message = "Username must be between 3 and 30 characters")
        String username,

        @Size(min = 1, max = 100, message = "Display name must be between 1 and 100 characters")
        @Pattern(regexp = ".*\\S.*", message = "Display name must not be blank")
        String displayName,

        @Size(max = 160, message = "Bio must not exceed 160 characters")
        String bio,

        @Size(max = 2048, message = "Profile picture URL must not exceed 2048 characters")
        @Pattern(regexp = "^$|^https?://\\S+$", message = "Profile picture must be an HTTP or HTTPS URL")
        String profilePicture,

        @Size(max = 2048, message = "Cover picture URL must not exceed 2048 characters")
        @Pattern(regexp = "^$|^https?://\\S+$", message = "Cover picture must be an HTTP or HTTPS URL")
        String coverPicture,

        UUID profileMediaId,

        UUID coverMediaId,

        @Size(max = 100, message = "Location must not exceed 100 characters")
        String location,

        @Size(max = 2048, message = "Website URL must not exceed 2048 characters")
        @Pattern(
                regexp = "^$|^(?:https?://)?(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\\.)+[A-Za-z]{2,63}(?:[/?#][^\\s]*)?$",
                message = "Website must be a valid web address")
        String website,

        @Past(message = "Date of birth must be in the past")
        LocalDate dateOfBirth,

        Boolean showLikesOnProfile) {
}
