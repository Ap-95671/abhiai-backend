package com.abhiai.abhiai_backend.dto.repost;

import java.util.UUID;

public record PostRepostStatusResponse(UUID postId, boolean reposted) {
}
