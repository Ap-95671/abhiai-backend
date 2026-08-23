package com.abhiai.abhiai_backend.dto.media;

public enum MediaKind {
    IMAGE,
    VIDEO,
    DOCUMENT;

    public static MediaKind fromContentType(String contentType) {
        if (contentType.startsWith("image/")) return IMAGE;
        if (contentType.startsWith("video/")) return VIDEO;
        return DOCUMENT;
    }
}
