package com.abhiai.abhiai_backend.ai.openai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.abhiai.abhiai_backend.exception.AiProviderException;

import tools.jackson.databind.ObjectMapper;

class OpenAiResponsesProviderTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void extractsTextFromAssistantMessageItems() throws Exception {
        String response = """
                {
                  "output": [
                    {"type": "reasoning", "content": []},
                    {
                      "type": "message",
                      "content": [
                        {"type": "output_text", "text": "Hello"},
                        {"type": "output_text", "text": " there"}
                      ]
                    }
                  ]
                }
                """;

        assertEquals("Hello there", OpenAiResponsesProvider.extractAssistantText(objectMapper.readTree(response)));
    }

    @Test
    void rejectsResponsesWithoutAssistantText() throws Exception {
        assertThrows(AiProviderException.class,
                () -> OpenAiResponsesProvider.extractAssistantText(objectMapper.readTree("{\"output\": []}")));
    }
}
