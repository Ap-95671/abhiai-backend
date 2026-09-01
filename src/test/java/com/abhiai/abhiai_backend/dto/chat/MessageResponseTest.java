package com.abhiai.abhiai_backend.dto.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.abhiai.abhiai_backend.entity.Conversation;
import com.abhiai.abhiai_backend.entity.Message;
import com.abhiai.abhiai_backend.entity.MessageCitation;
import com.abhiai.abhiai_backend.entity.MessageRole;
import com.abhiai.abhiai_backend.entity.User;

class MessageResponseTest {

    @Test
    void includesPersistentStructuredCitations() {
        User user = new User("abhishek", "Abhishek", "user@example.com", "hash");
        Message message = new Message(new Conversation(user, "Research"), MessageRole.ASSISTANT, "Answer");
        message.replaceCitations(List.of(new MessageCitation("Example source", "https://example.com/report", "example.com")));

        MessageResponse response = MessageResponse.from(message);

        assertEquals(1, response.citations().size());
        assertEquals("Example source", response.citations().getFirst().title());
        assertEquals("https://example.com/report", response.citations().getFirst().url());
    }
}
