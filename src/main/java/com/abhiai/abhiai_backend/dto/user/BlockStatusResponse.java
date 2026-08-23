package com.abhiai.abhiai_backend.dto.user;

import java.util.UUID;

public record BlockStatusResponse(UUID userId, boolean blockedByMe, boolean blockedMe) {}
