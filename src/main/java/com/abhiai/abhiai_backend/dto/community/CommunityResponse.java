package com.abhiai.abhiai_backend.dto.community;

import java.time.Instant;
import java.util.UUID;

import com.abhiai.abhiai_backend.dto.post.PostAuthorResponse;
import com.abhiai.abhiai_backend.entity.Community;
import com.abhiai.abhiai_backend.entity.CommunityPrivacy;
import com.abhiai.abhiai_backend.entity.CommunityRole;

public record CommunityResponse(
        UUID id,
        String name,
        String slug,
        String description,
        String iconUrl,
        String bannerUrl,
        PostAuthorResponse owner,
        long memberCount,
        CommunityPrivacy privacy,
        boolean joined,
        CommunityRole currentUserRole,
        Instant createdAt,
        Instant updatedAt) {

    public static CommunityResponse from(Community community, CommunityRole currentUserRole) {
        return new CommunityResponse(
                community.getId(),
                community.getName(),
                community.getSlug(),
                community.getDescription(),
                community.getIconUrl(),
                community.getBannerUrl(),
                PostAuthorResponse.from(community.getOwner()),
                community.getMemberCount(),
                community.getPrivacy(),
                currentUserRole != null,
                currentUserRole,
                community.getCreatedAt(),
                community.getUpdatedAt());
    }
}
