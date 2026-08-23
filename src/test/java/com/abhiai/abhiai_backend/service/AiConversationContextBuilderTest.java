package com.abhiai.abhiai_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.abhiai.abhiai_backend.ai.AiChatMessage;
import com.abhiai.abhiai_backend.config.AiContextProperties;
import com.abhiai.abhiai_backend.entity.Conversation;
import com.abhiai.abhiai_backend.entity.Message;
import com.abhiai.abhiai_backend.entity.MessageRole;
import com.abhiai.abhiai_backend.entity.User;

class AiConversationContextBuilderTest {

    @Test
    void keepsNewestOrderedContextWithinLimits() {
        AiContextProperties properties = new AiContextProperties();
        properties.setMaxMessages(3);
        properties.setMaxCharacters(19);
        AiConversationContextBuilder builder = new AiConversationContextBuilder(properties);
        Conversation conversation = new Conversation(
                new User("user", "User", "u@example.com", "hash"),
                "Chat");

        var result = builder.build(
                List.of(
                        new Message(conversation, MessageRole.USER, "old message"),
                        new Message(conversation, MessageRole.ASSISTANT, "recent")),
                new AiChatMessage(MessageRole.USER, "now"));

        assertEquals(2, result.size());
        assertEquals("recent", result.get(0).content());
        assertEquals("now", result.get(1).content());
    }

    @Test
    void alwaysIncludesCurrentMessage() {
        AiContextProperties properties = new AiContextProperties();
        properties.setMaxCharacters(1);

        var result = new AiConversationContextBuilder(properties).build(
                List.of(),
                new AiChatMessage(MessageRole.USER, "long current message"));

        assertEquals(1, result.size());
        assertEquals("long current message", result.getFirst().content());
    }
}
