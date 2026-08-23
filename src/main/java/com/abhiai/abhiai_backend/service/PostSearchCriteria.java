package com.abhiai.abhiai_backend.service;

import java.time.Instant;

public record PostSearchCriteria(
        String authorUsername,
        Instant fromDate,
        Instant toDate,
        Boolean hasMedia,
        SearchSort sort) {

    public static PostSearchCriteria defaults() {
        return new PostSearchCriteria(null, null, null, null, SearchSort.RELEVANCE);
    }
}
