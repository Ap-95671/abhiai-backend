package com.abhiai.abhiai_backend.news.model;

import java.util.Locale;

import com.abhiai.abhiai_backend.news.exception.InvalidNewsQueryException;

public enum NewsRegion {
    GLOBAL("global", null),
    INDIA("india", "in"),
    US("us", "us"),
    EUROPE("europe", "gb,de,fr,it,es"),
    ASIA("asia", "in,jp,sg,kr,cn"),
    MIDDLE_EAST("middle-east", "ae,sa,il,qa,eg"),
    AFRICA("africa", "za,ng,ke,eg,ma"),
    AMERICAS("americas", "us,ca,br,mx,ar");

    private final String id;
    private final String countryCodes;

    NewsRegion(String id, String countryCodes) {
        this.id = id;
        this.countryCodes = countryCodes;
    }

    public String id() { return id; }
    public String countryCodes() { return countryCodes; }

    public static NewsRegion parse(String value) {
        String normalized = value == null || value.isBlank()
                ? GLOBAL.id
                : value.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        for (NewsRegion region : values()) if (region.id.equals(normalized)) return region;
        throw new InvalidNewsQueryException("Unsupported news region: " + value);
    }
}
