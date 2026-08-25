package com.abhiai.abhiai_backend.ai.image;

public record GeneratedImage(byte[] content, String contentType, String model) {
    public GeneratedImage {
        content = content.clone();
    }

    @Override
    public byte[] content() {
        return content.clone();
    }
}
