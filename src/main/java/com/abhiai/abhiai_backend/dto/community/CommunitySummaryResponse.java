package com.abhiai.abhiai_backend.dto.community;

import java.util.UUID;

import com.abhiai.abhiai_backend.entity.Community;

public record CommunitySummaryResponse(
        UUID id,
        String name,
        String slug,
        String iconUrl) {

    public static CommunitySummaryResponse from(Community community) {
        return new CommunitySummaryResponse(
                community.getId(),
                community.getName(),
                community.getSlug(),
                community.getIconUrl());
    }
}
