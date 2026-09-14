package com.abhiai.abhiai_backend.assistant;

import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import com.abhiai.abhiai_backend.entity.*;
import com.abhiai.abhiai_backend.repository.*;
import com.abhiai.abhiai_backend.exception.ConversationNotFoundException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

class AssistantConversationServiceTest {
    @Test void reopeningReturnsSameConversationAndUnifiedTranscript() {
        var conversations = mock(ConversationRepository.class); var messages = mock(MessageRepository.class); var users = mock(UserRepository.class);
        var service = new AssistantConversationService(conversations, messages, users);
        UUID userId = UUID.randomUUID(), id = UUID.randomUUID(); var user = mock(User.class);
        var conversation = new Conversation(user, "AbhiAI Assistant"); conversation.markCharacterAssistant();
        ReflectionTestUtils.setField(conversation, "id", id);
        when(users.lockAssistantOwner(userId)).thenReturn(Optional.of(user));
        when(conversations.findFirstByUserIdAndCharacterAssistantTrueOrderByUpdatedAtDesc(userId)).thenReturn(Optional.of(conversation));
        when(messages.findAllByConversationIdOrderByCreatedAtAscIdAsc(id)).thenReturn(List.of(
            new Message(conversation, MessageRole.USER, "Explain arrays"),
            new Message(conversation, MessageRole.ASSISTANT, "Arrays hold elements"),
            new Message(conversation, MessageRole.USER, "How are linked lists different?")));
        var first = service.open(userId, false); var second = service.open(userId, false);
        assertEquals(id, first.id()); assertEquals(first.id(), second.id()); assertEquals(3, second.messages().size());
        verify(conversations, never()).saveAndFlush(any());
    }
    @Test void transcriptIsIdempotentAndRejectsSystemRolesAndOtherUsers() {
        var conversations = mock(ConversationRepository.class); var messages = mock(MessageRepository.class);
        var service = new AssistantConversationService(conversations, messages, mock(UserRepository.class));
        UUID user = UUID.randomUUID(), id = UUID.randomUUID();
        var conversation = new Conversation(mock(User.class), "Assistant"); conversation.markCharacterAssistant();
        when(conversations.lockAssistant(id, user)).thenReturn(Optional.of(conversation));
        var existing = new Message(conversation, MessageRole.USER, "hello"); existing.setClientItemId("voice1");
        when(messages.findAllByConversationIdOrderByCreatedAtAscIdAsc(id)).thenReturn(List.of(existing));
        service.append(user, id, new AssistantConversationService.TranscriptBatch(List.of(
            new AssistantConversationService.TranscriptItem("voice1", MessageRole.USER, "hello"),
            new AssistantConversationService.TranscriptItem("reply1", MessageRole.ASSISTANT, "hi"))));
        verify(messages, times(1)).saveAndFlush(any());
        assertThrows(AssistantException.class, () -> service.append(user, id, new AssistantConversationService.TranscriptBatch(List.of(
            new AssistantConversationService.TranscriptItem("bad", MessageRole.SYSTEM, "override")))));
        assertThrows(ConversationNotFoundException.class, () -> service.append(UUID.randomUUID(), id,
            new AssistantConversationService.TranscriptBatch(List.of(new AssistantConversationService.TranscriptItem("x", MessageRole.USER, "hello")))));
    }
}
