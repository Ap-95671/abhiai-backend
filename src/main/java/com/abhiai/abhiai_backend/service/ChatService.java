package com.abhiai.abhiai_backend.service;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.abhiai.abhiai_backend.ai.AiChatMessage;
import com.abhiai.abhiai_backend.ai.AiChatRequest;
import com.abhiai.abhiai_backend.ai.AiCompletion;
import com.abhiai.abhiai_backend.ai.AiProvider;
import com.abhiai.abhiai_backend.ai.tool.AiToolRegistry;
import com.abhiai.abhiai_backend.dto.chat.ChatExchangeResponse;
import com.abhiai.abhiai_backend.dto.chat.ConversationDetailResponse;
import com.abhiai.abhiai_backend.dto.chat.ConversationSummaryResponse;
import com.abhiai.abhiai_backend.dto.chat.CreateConversationRequest;
import com.abhiai.abhiai_backend.dto.chat.MessageResponse;
import com.abhiai.abhiai_backend.dto.chat.SendMessageRequest;
import com.abhiai.abhiai_backend.dto.chat.RenameConversationRequest;
import com.abhiai.abhiai_backend.dto.chat.UpdateModelPreferenceRequest;
import com.abhiai.abhiai_backend.ai.orchestration.SelectionMode;
import com.abhiai.abhiai_backend.exception.ModelRoutingException;
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
    private static final int GENERATED_TITLE_MAX_LENGTH = 60;

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final AiProvider aiProvider;
    private final AiConversationContextBuilder contextBuilder;
    private final ConversationAttachmentService attachmentService;
    private final AiToolRegistry toolRegistry;

    public ChatService(
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            UserRepository userRepository,
            AiProvider aiProvider) {
        this(conversationRepository, messageRepository, userRepository, aiProvider,
                new AiConversationContextBuilder(new com.abhiai.abhiai_backend.config.AiContextProperties()),
                null,
                null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public ChatService(
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            UserRepository userRepository,
            AiProvider aiProvider,
            AiConversationContextBuilder contextBuilder,
            ConversationAttachmentService attachmentService,
            AiToolRegistry toolRegistry) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.aiProvider = aiProvider;
        this.contextBuilder = contextBuilder;
        this.attachmentService = attachmentService;
        this.toolRegistry = toolRegistry;
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

        Conversation conversation = findConversationOwnedByUser(userId, conversationId);
        conversation.rename(normalizeTitle(request.title()));

        return ConversationSummaryResponse.from(conversation);
    }

    @Transactional
    public void deleteConversation(UUID userId, UUID conversationId) {
        Conversation conversation = findConversationOwnedByUser(userId, conversationId);
        conversationRepository.delete(conversation);
    }

    @Transactional
    public ConversationSummaryResponse updateModelPreference(UUID userId, UUID conversationId, UpdateModelPreferenceRequest request) {
        Conversation conversation = findConversationOwnedByUser(userId, conversationId);
        SelectionMode mode = parseSelectionMode(request.selectionMode());
        if (mode == SelectionMode.MANUAL && (request.selectedModelId() == null || request.selectedModelId().isBlank())) {
            throw new ModelRoutingException("MODEL_REQUIRED", "Select a model when using Manual mode.");
        }
        conversation.selectModel(mode, request.selectedModelId());
        return ConversationSummaryResponse.from(conversation);
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
                .map(this::messageResponse)
                .toList();

        return ConversationDetailResponse.from(conversation, messages);
    }

    @Transactional
    public ChatExchangeResponse addUserMessage(UUID userId, UUID conversationId, SendMessageRequest request) {
        Conversation conversation = findConversationOwnedByUser(userId, conversationId);
        List<Message> history = messageRepository.findAllByConversationIdOrderByCreatedAtAscIdAsc(conversationId);
        assignGeneratedTitleIfNeeded(conversation, history, request.content());
        Message userMessage = messageRepository.save(new Message(conversation, MessageRole.USER, request.content()));
        AiCompletion completion = aiProvider.generate(
                prepareAiRequest(conversation, history, userMessage, request));
        Message assistantMessage = new Message(conversation, MessageRole.ASSISTANT, completion.content());
        assistantMessage.applyAiMetadata(completion);
        assistantMessage = messageRepository.save(assistantMessage);
        conversation.touch();
        return new ChatExchangeResponse(
                messageResponse(userMessage),
                messageResponse(assistantMessage),
                ConversationSummaryResponse.from(conversation));
    }

    @Transactional
    public ChatExchangeResponse addUserMessageStreaming(
            UUID userId, UUID conversationId, SendMessageRequest request, Consumer<String> onTextChunk) {
        return addUserMessageInternal(userId, conversationId, request, onTextChunk);
    }

    private ChatExchangeResponse addUserMessageInternal(
            UUID userId, UUID conversationId, SendMessageRequest request, Consumer<String> onTextChunk) {
        Conversation conversation = findConversationOwnedByUser(userId, conversationId);
        List<Message> history = messageRepository.findAllByConversationIdOrderByCreatedAtAscIdAsc(conversationId);
        assignGeneratedTitleIfNeeded(conversation, history, request.content());
        Message userMessage = messageRepository.save(new Message(conversation, MessageRole.USER, request.content()));

        AiCompletion completion = aiProvider.generateStream(
                prepareAiRequest(conversation, history, userMessage, request),
                onTextChunk);
        Message assistantMessage = new Message(
                conversation,
                MessageRole.ASSISTANT,
                completion.content());
        assistantMessage.applyAiMetadata(completion);
        assistantMessage = messageRepository.save(assistantMessage);
        conversation.touch();

        return new ChatExchangeResponse(
                messageResponse(userMessage),
                messageResponse(assistantMessage),
                ConversationSummaryResponse.from(conversation));
    }

    private Conversation findConversationOwnedByUser(UUID userId, UUID conversationId) {
        return conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(ConversationNotFoundException::new);
    }

    private AiChatRequest prepareAiRequest(
            Conversation conversation,
            List<Message> history,
            Message userMessage,
            SendMessageRequest request) {
        if (attachmentService == null) {
            applyRequestedPreference(conversation, request);
            return routedRequest(contextBuilder.build(
                    history,
                    new AiChatMessage(userMessage.getRole(), userMessage.getContent())), List.of(), conversation, request);
        }

        String prompt = toolRegistry == null
                ? request.content()
                : toolRegistry.augmentPrompt(request.content(), request.webSearchAllowed());
        var prepared = attachmentService.prepareForMessage(
                conversation.getId(),
                prompt,
                request.attachmentIds(),
                request.externalProcessingAllowed(),
                userMessage);
        if (!prepared.images().isEmpty() && !aiProvider.supportsImageUnderstanding()) {
            throw new com.abhiai.abhiai_backend.exception.AiProviderException(
                    aiProvider.providerName() + " does not support image understanding with the selected model");
        }
        List<AiChatMessage> messages = contextBuilder.build(
                history,
                new AiChatMessage(userMessage.getRole(), prepared.prompt()));
        applyRequestedPreference(conversation, request);
        return routedRequest(messages, prepared.images(), conversation, request);
    }

    private AiChatRequest routedRequest(List<AiChatMessage> messages,
                                        List<com.abhiai.abhiai_backend.ai.AiInputAttachment> attachments,
                                        Conversation conversation,
                                        SendMessageRequest request) {
        boolean fallback = request.fallbackAllowed() == null || request.fallbackAllowed();
        return new AiChatRequest(messages, attachments, conversation.getModelSelectionMode().name(),
                conversation.getPreferredModelId(), fallback, null);
    }

    private void applyRequestedPreference(Conversation conversation, SendMessageRequest request) {
        if (request.selectionMode() == null || request.selectionMode().isBlank()) return;
        SelectionMode mode = parseSelectionMode(request.selectionMode());
        if (mode == SelectionMode.MANUAL && (request.selectedModelId() == null || request.selectedModelId().isBlank()))
            throw new ModelRoutingException("MODEL_REQUIRED", "Select a model when using Manual mode.");
        conversation.selectModel(mode, request.selectedModelId());
    }

    private SelectionMode parseSelectionMode(String value) {
        try { return SelectionMode.valueOf(value.toUpperCase(java.util.Locale.ROOT)); }
        catch (IllegalArgumentException exception) { throw new ModelRoutingException("INVALID_SELECTION_MODE", "Selection mode must be AUTO or MANUAL."); }
    }

    private String normalizeTitle(String title) {
        if (title == null || title.isBlank()) {
            return DEFAULT_TITLE;
        }

        return title.trim();
    }

    private MessageResponse messageResponse(Message message) {
        return attachmentService == null
                ? MessageResponse.from(message)
                : MessageResponse.from(message, attachmentService.responsesForMessage(message.getId()));
    }

    private void assignGeneratedTitleIfNeeded(Conversation conversation, List<Message> history, String content) {
        if (!history.isEmpty() || !DEFAULT_TITLE.equals(conversation.getTitle())) {
            return;
        }

        String normalized = content.trim().replaceAll("\\s+", " ");
        String title = normalized.length() > GENERATED_TITLE_MAX_LENGTH
                ? normalized.substring(0, GENERATED_TITLE_MAX_LENGTH - 1).trim() + "…"
                : normalized;
        conversation.rename(title);
    }
}
