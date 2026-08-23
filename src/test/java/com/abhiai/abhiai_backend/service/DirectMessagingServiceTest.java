package com.abhiai.abhiai_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import com.abhiai.abhiai_backend.entity.DirectConversation;
import com.abhiai.abhiai_backend.entity.DirectMessage;
import com.abhiai.abhiai_backend.entity.MessageReadReceipt;
import com.abhiai.abhiai_backend.entity.User;
import com.abhiai.abhiai_backend.exception.InvalidDirectMessageException;
import com.abhiai.abhiai_backend.exception.UnauthorizedActionException;
import com.abhiai.abhiai_backend.repository.DirectConversationRepository;
import com.abhiai.abhiai_backend.repository.DirectMessageRepository;
import com.abhiai.abhiai_backend.repository.MessageReadReceiptRepository;
import com.abhiai.abhiai_backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class DirectMessagingServiceTest {

    private static final UUID ACTING_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID CONVERSATION_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-08-17T16:00:00Z");

    @Mock private DirectConversationRepository conversationRepository;
    @Mock private DirectMessageRepository messageRepository;
    @Mock private MessageReadReceiptRepository receiptRepository;
    @Mock private UserRepository userRepository;

    private DirectMessagingService service;
    private User actingUser;
    private User otherUser;
    private DirectConversation conversation;

    @BeforeEach
    void setUp() {
        service = new DirectMessagingService(
                conversationRepository,
                messageRepository,
                receiptRepository,
                userRepository,
                Clock.fixed(NOW, ZoneOffset.UTC));
        actingUser = user(ACTING_ID, "acting_user", "Acting User");
        otherUser = user(OTHER_ID, "other_user", "Other User");
        conversation = conversation();
    }

    @Test
    void startsOneConversationForAnOrderedParticipantPair() {
        when(userRepository.findById(ACTING_ID)).thenReturn(Optional.of(actingUser));
        when(userRepository.findByUsernameIgnoreCase("other_user")).thenReturn(Optional.of(otherUser));
        when(conversationRepository.findByParticipantKey(
                ACTING_ID + ":" + OTHER_ID)).thenReturn(Optional.empty());
        when(conversationRepository.saveAndFlush(any(DirectConversation.class))).thenAnswer(invocation -> {
            DirectConversation saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", CONVERSATION_ID);
            ReflectionTestUtils.setField(saved, "createdAt", NOW);
            ReflectionTestUtils.setField(saved, "updatedAt", NOW);
            return saved;
        });

        var result = service.startConversation(ACTING_ID, " @Other_User ");

        assertEquals(CONVERSATION_ID, result.id());
        assertEquals("other_user", result.participant().username());
        assertEquals(0, result.unreadCount());
    }

    @Test
    void rejectsConversationWithSelf() {
        when(userRepository.findById(ACTING_ID)).thenReturn(Optional.of(actingUser));
        when(userRepository.findByUsernameIgnoreCase("acting_user")).thenReturn(Optional.of(actingUser));

        assertThrows(InvalidDirectMessageException.class,
                () -> service.startConversation(ACTING_ID, "acting_user"));
    }

    @Test
    void sendsTrimmedMessageAndTouchesConversation() {
        when(conversationRepository.findAccessible(CONVERSATION_ID, ACTING_ID))
                .thenReturn(Optional.of(conversation));
        when(userRepository.findById(ACTING_ID)).thenReturn(Optional.of(actingUser));
        when(messageRepository.saveAndFlush(any(DirectMessage.class))).thenAnswer(invocation -> {
            DirectMessage saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", UUID.randomUUID());
            ReflectionTestUtils.setField(saved, "createdAt", NOW);
            return saved;
        });

        var result = service.sendMessage(ACTING_ID, CONVERSATION_ID, "  hello there  ");

        assertEquals("hello there", result.content());
        assertFalse(result.readByRecipient());
        ArgumentCaptor<DirectMessage> message = ArgumentCaptor.forClass(DirectMessage.class);
        verify(messageRepository).saveAndFlush(message.capture());
        assertEquals("hello there", message.getValue().getContent());
    }

    @Test
    void marksOnlyUnreadIncomingMessagesAsRead() {
        DirectMessage incoming = directMessage(otherUser, "hello");
        when(conversationRepository.findAccessible(CONVERSATION_ID, ACTING_ID))
                .thenReturn(Optional.of(conversation));
        when(userRepository.findById(ACTING_ID)).thenReturn(Optional.of(actingUser));
        when(messageRepository.findUnreadForUser(CONVERSATION_ID, ACTING_ID))
                .thenReturn(List.of(incoming));

        var result = service.markConversationRead(ACTING_ID, CONVERSATION_ID);

        assertEquals(1, result.updatedCount());
        assertEquals(NOW, result.readAt());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MessageReadReceipt>> receipts = ArgumentCaptor.forClass(List.class);
        verify(receiptRepository).saveAll(receipts.capture());
        assertEquals(1, receipts.getValue().size());
    }

    @Test
    void returnsNewestFirstHistoryWithRecipientReadState() {
        DirectMessage message = directMessage(actingUser, "already seen");
        when(conversationRepository.findAccessible(CONVERSATION_ID, ACTING_ID))
                .thenReturn(Optional.of(conversation));
        when(messageRepository.findAllByConversationIdOrderByCreatedAtDescIdDesc(
                CONVERSATION_ID, PageRequest.of(0, 50)))
                .thenReturn(new PageImpl<>(List.of(message), PageRequest.of(0, 50), 1));
        when(receiptRepository.existsByMessageIdAndUserId(message.getId(), OTHER_ID)).thenReturn(true);

        var result = service.getHistory(ACTING_ID, CONVERSATION_ID, PageRequest.of(0, 50));

        assertEquals(1, result.totalElements());
        assertEquals(true, result.content().getFirst().readByRecipient());
    }

    @Test
    void preventsDeletingAnotherUsersMessage() {
        DirectMessage message = directMessage(otherUser, "not yours");
        when(conversationRepository.findAccessible(CONVERSATION_ID, ACTING_ID))
                .thenReturn(Optional.of(conversation));
        when(messageRepository.findByIdAndConversationId(message.getId(), CONVERSATION_ID))
                .thenReturn(Optional.of(message));

        assertThrows(UnauthorizedActionException.class,
                () -> service.deleteMessage(ACTING_ID, CONVERSATION_ID, message.getId()));
    }

    private User user(UUID id, String username, String displayName) {
        User user = new User(username, displayName, username + "@example.com", "hash");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private DirectConversation conversation() {
        DirectConversation value = new DirectConversation(ACTING_ID + ":" + OTHER_ID);
        value.addParticipant(actingUser);
        value.addParticipant(otherUser);
        ReflectionTestUtils.setField(value, "id", CONVERSATION_ID);
        ReflectionTestUtils.setField(value, "createdAt", NOW);
        ReflectionTestUtils.setField(value, "updatedAt", NOW);
        return value;
    }

    private DirectMessage directMessage(User sender, String content) {
        DirectMessage message = new DirectMessage(conversation, sender, content);
        ReflectionTestUtils.setField(message, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(message, "createdAt", NOW);
        return message;
    }
}
