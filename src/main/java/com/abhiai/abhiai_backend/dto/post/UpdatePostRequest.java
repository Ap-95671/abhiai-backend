package com.abhiai.abhiai_backend.dto.post;

import com.abhiai.abhiai_backend.entity.PostVisibility;

public record UpdatePostRequest(
        String textContent,

        PostVisibility visibility) {
}
