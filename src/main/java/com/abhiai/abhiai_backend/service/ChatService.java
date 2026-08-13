package com.abhiai.abhiai_backend.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.abhiai.abhiai_backend.ai.AiChatMessage;
import com.abhiai.abhiai_backend.ai.AiChatRequest;
import com.abhiai.abhiai_backend.ai.AiCompletion;
import com.abhiai.abhiai_backend.ai.AiProvider;
import com.abhiai.abhiai_backend.dto.chat.ChatExchangeResponse;
import com.abhiai.abhiai_backend.dto.chat.ConversationDetailResponse;
import com.abhiai.abhiai_backend.dto.chat.ConversationSummaryResponse;
import com.abhiai.abhiai_backend.dto.chat.CreateConversationRequest;
import com.abhiai.abhiai_backend.dto.chat.MessageResponse;
import com.abhiai.abhiai_backend.dto.chat.SendMessageRequest;
import com.abhiai.abhiai_backend.dto.chat.RenameConversationRequest;
import com.abhiai.abhiai_backend.entity.Conversation;
import com.abhiai.abhiai_backend.entity.Message;
import com.abhiai.abhiai_backend.entity.MessageRole;
import com.abhiai.abhiai_backend.entity.User;
import com.abhiai.abhiai_backend.exception.ConversationNotFoundException;
import com.abhiai.abhiai_backend.exception.InvalidCredentialsException;
import com.abhiai.abhiai_backend.repository.ConversationRepository;
import com.abhiai.abhiai_backend.repository.MessageRepository;
import com.abhiai.abhiai_backend.repository.UserRepository;

@Service
public class ChatService {

    private static final String DEFAULT_TITLE = "New conversation";

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final AiProvider aiProvider;

    public ChatService(
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            UserRepository userRepository,
            AiProvider aiProvider) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.aiProvider = aiProvider;
    }

    @Transactional
    public ConversationSummaryResponse createConversation(UUID userId, CreateConversationRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(InvalidCredentialsException::new);
        Conversation conversation = new Conversation(user, normalizeTitle(request.title()));

        return ConversationSummaryResponse.from(conversationRepository.save(conversation));
    }
    @Transactional
public ConversationSummaryResponse renameConversation(
        UUID userId,
        UUID conversationId,
        RenameConversationRequest request) {

    Conversation conversation =
            findConversationOwnedByUser(userId, conversationId);

    conversation.rename(normalizeTitle(request.title()));

    return ConversationSummaryResponse.from(conversation);
}

@Transactional
public void deleteConversation(
        UUID userId,
        UUID conversationId) {

    Conversation conversation =
            findConversationOwnedByUser(userId, conversationId);

    conversationRepository.delete(conversation);
}

    @Transactional(readOnly = true)
    public List<ConversationSummaryResponse> getConversations(UUID userId) {
        return conversationRepository.findAllByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(ConversationSummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ConversationDetailResponse getConversationHistory(UUID userId, UUID conversationId) {
        Conversation conversation = findConversationOwnedByUser(userId, conversationId);
        List<MessageResponse> messages = messageRepository
                .findAllByConversationIdOrderByCreatedAtAscIdAsc(conversationId)
                .stream()
                .map(MessageResponse::from)
                .toList();

        return ConversationDetailResponse.from(conversation, messages);
    }

    @Transactional
    public ChatExchangeResponse addUserMessage(UUID userId, UUID conversationId, SendMessageRequest request) {
        Conversation conversation = findConversationOwnedByUser(userId, conversationId);
        List<Message> history = messageRepository.findAllByConversationIdOrderByCreatedAtAscIdAsc(conversationId);
        Message userMessage = messageRepository.save(new Message(conversation, MessageRole.USER, request.content()));

        List<AiChatMessage> providerMessages = new ArrayList<>(history.stream()
                .map(message -> new AiChatMessage(message.getRole(), message.getContent()))
                .toList());
        providerMessages.add(new AiChatMessage(userMessage.getRole(), userMessage.getContent()));

        AiCompletion completion = aiProvider.generate(new AiChatRequest(providerMessages));
        Message assistantMessage = messageRepository.save(new Message(
                conversation,
                MessageRole.ASSISTANT,
                completion.content()));
        conversation.touch();

        return new ChatExchangeResponse(
                MessageResponse.from(userMessage),
                MessageResponse.from(assistantMessage));
    }

    private Conversation findConversationOwnedByUser(UUID userId, UUID conversationId) {
        return conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(ConversationNotFoundException::new);
    }

    private String normalizeTitle(String title) {
        if (title == null || title.isBlank()) {
            return DEFAULT_TITLE;
        }

        return title.trim();
    }
}
