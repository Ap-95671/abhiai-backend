package com.abhiai.abhiai_backend.dto.ai;

import java.util.Set;

public record ModelOptionResponse(
        String id, String provider, String displayName, String description, long contextWindow,
        Set<String> capabilities, String status, boolean configured) {
}
