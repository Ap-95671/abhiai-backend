package com.abhiai.abhiai_backend.ai;

import java.util.Arrays;

public record AiInputAttachment(String filename, String contentType, byte[] content) {
    public AiInputAttachment {
        content = Arrays.copyOf(content, content.length);
    }

    @Override
    public byte[] content() {
        return Arrays.copyOf(content, content.length);
    }
}
