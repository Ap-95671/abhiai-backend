package com.abhiai.abhiai_backend.dto.community;

import com.abhiai.abhiai_backend.entity.CommunityPrivacy;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCommunityRequest(
        @NotBlank(message = "Community name is required")
        @Size(max = 100, message = "Community name must not exceed 100 characters")
        String name,

        @Size(max = 64, message = "Community slug must not exceed 64 characters")
        String slug,

        @NotBlank(message = "Community description is required")
        @Size(max = 1000, message = "Community description must not exceed 1000 characters")
        String description,

        @Size(max = 2048, message = "Community icon URL must not exceed 2048 characters")
        String iconUrl,

        @Size(max = 2048, message = "Community banner URL must not exceed 2048 characters")
        String bannerUrl,

        CommunityPrivacy privacy) {
}
