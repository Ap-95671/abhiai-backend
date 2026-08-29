package com.abhiai.abhiai_backend.news.model;

public record NewsQuery(
        NewsCategory category,
        NewsRegion region,
        String language,
        String query,
        int page,
        int limit) {

    public String cacheKey() {
        return category.id() + "|" + region.id() + "|" + language + "|" + query.toLowerCase();
    }

    public NewsQuery withoutPage() {
        return new NewsQuery(category, region, language, query, 0, limit);
    }
}
