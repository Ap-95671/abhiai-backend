package com.abhiai.abhiai_backend.news.model;

import java.util.Locale;

import com.abhiai.abhiai_backend.news.exception.InvalidNewsQueryException;

public enum NewsCategory {
    FOR_YOU("for-you", null),
    LATEST("latest", null),
    TOP("top", "top"),
    WORLD("world", "world"),
    AI_TECH("ai-tech", "technology"),
    BUSINESS("business", "business"),
    SCIENCE("science", "science"),
    POLITICS("politics", "politics"),
    SPORTS("sports", "sports"),
    ENTERTAINMENT("entertainment", "entertainment");

    private final String id;
    private final String providerValue;

    NewsCategory(String id, String providerValue) {
        this.id = id;
        this.providerValue = providerValue;
    }

    public String id() { return id; }
    public String providerValue() { return providerValue; }

    public static NewsCategory parse(String value) {
        String normalized = value == null || value.isBlank()
                ? LATEST.id
                : value.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        for (NewsCategory category : values()) if (category.id.equals(normalized)) return category;
        throw new InvalidNewsQueryException("Unsupported news category: " + value);
    }
}
