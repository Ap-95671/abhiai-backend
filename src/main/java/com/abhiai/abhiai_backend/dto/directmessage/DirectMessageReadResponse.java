package com.abhiai.abhiai_backend.dto.directmessage;

import java.time.Instant;

public record DirectMessageReadResponse(long updatedCount, Instant readAt) {
}
