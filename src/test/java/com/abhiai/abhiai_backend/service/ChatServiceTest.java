package com.abhiai.abhiai_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.doAnswer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.abhiai.abhiai_backend.ai.AiChatRequest;
import com.abhiai.abhiai_backend.ai.AiCompletion;
import com.abhiai.abhiai_backend.ai.AiProvider;
import com.abhiai.abhiai_backend.ai.orchestration.SelectionMode;
import com.abhiai.abhiai_backend.dto.chat.ChatExchangeResponse;
import com.abhiai.abhiai_backend.dto.chat.ConversationDetailResponse;
import com.abhiai.abhiai_backend.dto.chat.ConversationSummaryResponse;
import com.abhiai.abhiai_backend.dto.chat.CreateConversationRequest;
import com.abhiai.abhiai_backend.dto.chat.MessageResponse;
import com.abhiai.abhiai_backend.dto.chat.SendMessageRequest;
import com.abhiai.abhiai_backend.entity.Conversation;
import com.abhiai.abhiai_backend.entity.Message;
import com.abhiai.abhiai_backend.entity.MessageRole;
import com.abhiai.abhiai_backend.entity.User;
import com.abhiai.abhiai_backend.exception.ConversationNotFoundException;
import com.abhiai.abhiai_backend.repository.ConversationRepository;
import com.abhiai.abhiai_backend.repository.MessageRepository;
import com.abhiai.abhiai_backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID CONVERSATION_ID = UUID.randomUUID();

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AiProvider aiProvider;

    private ChatService chatService;

    @BeforeEach
    void setUp() {
        chatService = new ChatService(conversationRepository, messageRepository, userRepository, aiProvider);
    }

    @Test
    void createConversationUsesTheAuthenticatedUserAndNormalizesTitle() {
        User user = new User("abhishek", "Abhishek", "user@example.com", "bcrypt-hash");
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ConversationSummaryResponse response = chatService.createConversation(
                USER_ID,
                new CreateConversationRequest("  Project ideas  "));

        ArgumentCaptor<Conversation> conversationCaptor = ArgumentCaptor.forClass(Conversation.class);
        verify(conversationRepository).save(conversationCaptor.capture());
        assertEquals("Project ideas", conversationCaptor.getValue().getTitle());
        assertEquals("Project ideas", response.title());
    }

    @Test
    void addUserMessagePersistsBothSidesOfTheExchange() {
        Conversation conversation = new Conversation(
                new User("abhishek", "Abhishek", "user@example.com", "bcrypt-hash"),
                "Chat");
        when(conversationRepository.findByIdAndUserId(CONVERSATION_ID, USER_ID)).thenReturn(Optional.of(conversation));
        when(messageRepository.findAllByConversationIdOrderByCreatedAtAscIdAsc(CONVERSATION_ID)).thenReturn(List.of());
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(aiProvider.generate(any(AiChatRequest.class))).thenReturn(new AiCompletion("AI reply"));

        ChatExchangeResponse response = chatService.addUserMessage(
                USER_ID,
                CONVERSATION_ID,
                new SendMessageRequest("  Preserve this formatting  "));

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository, times(2)).save(messageCaptor.capture());
        assertEquals(MessageRole.USER, messageCaptor.getAllValues().get(0).getRole());
        assertEquals("  Preserve this formatting  ", messageCaptor.getAllValues().get(0).getContent());
        assertEquals(MessageRole.ASSISTANT, messageCaptor.getAllValues().get(1).getRole());
        assertEquals("AI reply", messageCaptor.getAllValues().get(1).getContent());
        assertEquals("  Preserve this formatting  ", response.userMessage().content());
        assertEquals("AI reply", response.assistantMessage().content());

        ArgumentCaptor<AiChatRequest> requestCaptor = ArgumentCaptor.forClass(AiChatRequest.class);
        verify(aiProvider).generate(requestCaptor.capture());
        assertEquals(1, requestCaptor.getValue().messages().size());
        assertEquals("  Preserve this formatting  ", requestCaptor.getValue().messages().get(0).content());
    }

    @Test
    void manualModelSelectionIsStrictByDefault() {
        Conversation conversation = new Conversation(
                new User("abhishek", "Abhishek", "user@example.com", "bcrypt-hash"),
                "Chat");
        conversation.selectModel(SelectionMode.MANUAL, "openai:gpt-test");
        when(conversationRepository.findByIdAndUserId(CONVERSATION_ID, USER_ID)).thenReturn(Optional.of(conversation));
        when(messageRepository.findAllByConversationIdOrderByCreatedAtAscIdAsc(CONVERSATION_ID)).thenReturn(List.of());
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(aiProvider.generate(any(AiChatRequest.class))).thenReturn(new AiCompletion("AI reply"));

        chatService.addUserMessage(USER_ID, CONVERSATION_ID, new SendMessageRequest("Hello"));

        ArgumentCaptor<AiChatRequest> requestCaptor = ArgumentCaptor.forClass(AiChatRequest.class);
        verify(aiProvider).generate(requestCaptor.capture());
        assertEquals("MANUAL", requestCaptor.getValue().selectionMode());
        assertEquals("openai:gpt-test", requestCaptor.getValue().selectedModelId());
        assertFalse(requestCaptor.getValue().fallbackAllowed());
    }

    @Test
    void automaticModelSelectionAllowsFallbackByDefault() {
        Conversation conversation = new Conversation(
                new User("abhishek", "Abhishek", "user@example.com", "bcrypt-hash"),
                "Chat");
        when(conversationRepository.findByIdAndUserId(CONVERSATION_ID, USER_ID)).thenReturn(Optional.of(conversation));
        when(messageRepository.findAllByConversationIdOrderByCreatedAtAscIdAsc(CONVERSATION_ID)).thenReturn(List.of());
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(aiProvider.generate(any(AiChatRequest.class))).thenReturn(new AiCompletion("AI reply"));

        chatService.addUserMessage(USER_ID, CONVERSATION_ID, new SendMessageRequest("Hello"));

        ArgumentCaptor<AiChatRequest> requestCaptor = ArgumentCaptor.forClass(AiChatRequest.class);
        verify(aiProvider).generate(requestCaptor.capture());
        assertEquals("AUTO", requestCaptor.getValue().selectionMode());
        assertTrue(requestCaptor.getValue().fallbackAllowed());
    }

    @Test
    void addUserMessageStreamingForwardsChunksAndPersistsTheCompletedExchange() {
        Conversation conversation = new Conversation(
                new User("abhishek", "Abhishek", "user@example.com", "bcrypt-hash"),
                "Chat");
        when(conversationRepository.findByIdAndUserId(CONVERSATION_ID, USER_ID)).thenReturn(Optional.of(conversation));
        when(messageRepository.findAllByConversationIdOrderByCreatedAtAscIdAsc(CONVERSATION_ID)).thenReturn(List.of());
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Consumer<String> onTextChunk = invocation.getArgument(1, Consumer.class);
            onTextChunk.accept("AI ");
            onTextChunk.accept("reply");
            return new AiCompletion("AI reply");
        }).when(aiProvider).generateStream(any(AiChatRequest.class), any());

        List<String> chunks = new ArrayList<>();
        ChatExchangeResponse response = chatService.addUserMessageStreaming(
                USER_ID,
                CONVERSATION_ID,
                new SendMessageRequest("Hello"),
                chunks::add);

        assertEquals(List.of("AI ", "reply"), chunks);
        assertEquals("Hello", response.userMessage().content());
        assertEquals("AI reply", response.assistantMessage().content());

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository, times(2)).save(messageCaptor.capture());
        assertEquals(MessageRole.ASSISTANT, messageCaptor.getAllValues().get(1).getRole());
        assertEquals("AI reply", messageCaptor.getAllValues().get(1).getContent());
    }

    @Test
    void getConversationHistoryReturnsMessagesInRepositoryOrder() {
        Conversation conversation = new Conversation(
                new User("abhishek", "Abhishek", "user@example.com", "bcrypt-hash"),
                "Chat");
        Message firstMessage = new Message(conversation, MessageRole.USER, "Hello");
        Message secondMessage = new Message(conversation, MessageRole.ASSISTANT, "Hi there");
        when(conversationRepository.findByIdAndUserId(CONVERSATION_ID, USER_ID)).thenReturn(Optional.of(conversation));
        when(messageRepository.findAllByConversationIdOrderByCreatedAtAscIdAsc(CONVERSATION_ID))
                .thenReturn(List.of(firstMessage, secondMessage));

        ConversationDetailResponse response = chatService.getConversationHistory(USER_ID, CONVERSATION_ID);

        assertEquals(2, response.messages().size());
        assertEquals("Hello", response.messages().get(0).content());
        assertEquals("Hi there", response.messages().get(1).content());
    }

    @Test
    void addUserMessageRejectsConversationOwnedByAnotherUser() {
        when(conversationRepository.findByIdAndUserId(CONVERSATION_ID, USER_ID)).thenReturn(Optional.empty());

        assertThrows(ConversationNotFoundException.class,
                () -> chatService.addUserMessage(USER_ID, CONVERSATION_ID, new SendMessageRequest("Hello")));

        verifyNoInteractions(messageRepository, aiProvider);
    }
}
