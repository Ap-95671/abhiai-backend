package com.abhiai.abhiai_backend.dto.ai;

import java.util.Map;

public record AiCapabilitiesResponse(
        String provider,
        String model,
        boolean configured,
        Map<String, Boolean> capabilities) {
}
