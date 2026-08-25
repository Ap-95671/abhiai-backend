package com.abhiai.abhiai_backend.ai.image;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.http.HttpClient;
import java.util.Base64;

import org.junit.jupiter.api.Test;

import com.abhiai.abhiai_backend.exception.AiProviderException;

import tools.jackson.databind.ObjectMapper;

class GeminiImageGenerationProviderTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final GeminiImageGenerationProvider provider = new GeminiImageGenerationProvider(
            HttpClient.newHttpClient(), objectMapper, new GeminiImageGenerationProperties());

    @Test
    void extractsImageFromInteractionSteps() {
        byte[] expected = new byte[] {(byte) 137, 80, 78, 71};
        String response = """
                {
                  "steps": [{
                    "type": "model_output",
                    "content": [{
                      "type": "image",
                      "data": "%s",
                      "mime_type": "image/png"
                    }]
                  }]
                }
                """.formatted(Base64.getEncoder().encodeToString(expected));

        GeneratedImage result = provider.extractGeneratedImage(objectMapper.readTree(response));

        assertArrayEquals(expected, result.content());
        assertEquals("image/png", result.contentType());
        assertEquals("gemini-3.1-flash-image", result.model());
    }

    @Test
    void rejectsInteractionWithoutImageOutput() {
        assertThrows(AiProviderException.class,
                () -> provider.extractGeneratedImage(objectMapper.readTree("{\"steps\": []}")));
    }
}
