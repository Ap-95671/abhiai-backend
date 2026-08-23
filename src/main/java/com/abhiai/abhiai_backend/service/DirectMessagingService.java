package com.abhiai.abhiai_backend.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.abhiai.abhiai_backend.dto.common.PageResponse;
import com.abhiai.abhiai_backend.dto.directmessage.DirectConversationResponse;
import com.abhiai.abhiai_backend.dto.directmessage.DirectMessageReadResponse;
import com.abhiai.abhiai_backend.dto.directmessage.DirectMessageResponse;
import com.abhiai.abhiai_backend.dto.post.PostAuthorResponse;
import com.abhiai.abhiai_backend.entity.ConversationParticipant;
import com.abhiai.abhiai_backend.entity.DirectConversation;
import com.abhiai.abhiai_backend.entity.DirectMessage;
import com.abhiai.abhiai_backend.entity.MessageReadReceipt;
import com.abhiai.abhiai_backend.entity.User;
import com.abhiai.abhiai_backend.exception.DirectConversationNotFoundException;
import com.abhiai.abhiai_backend.exception.DirectMessageNotFoundException;
import com.abhiai.abhiai_backend.exception.InvalidDirectMessageException;
import com.abhiai.abhiai_backend.exception.UnauthorizedActionException;
import com.abhiai.abhiai_backend.exception.UserNotFoundException;
import com.abhiai.abhiai_backend.repository.DirectConversationRepository;
import com.abhiai.abhiai_backend.repository.DirectMessageRepository;
import com.abhiai.abhiai_backend.repository.MessageReadReceiptRepository;
import com.abhiai.abhiai_backend.repository.UserRepository;

@Service
public class DirectMessagingService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_MESSAGE_LENGTH = 2000;

    private final DirectConversationRepository conversationRepository;
    private final DirectMessageRepository messageRepository;
    private final MessageReadReceiptRepository receiptRepository;
    private final UserRepository userRepository;
    private final Clock clock;
    private final BlockPolicyService blockPolicyService;

    public DirectMessagingService(
            DirectConversationRepository conversationRepository,
            DirectMessageRepository messageRepository,
            MessageReadReceiptRepository receiptRepository,
            UserRepository userRepository) {
        this(conversationRepository, messageRepository, receiptRepository, userRepository, Clock.systemUTC(), null);
    }

    @Autowired
    public DirectMessagingService(DirectConversationRepository conversationRepository,
            DirectMessageRepository messageRepository, MessageReadReceiptRepository receiptRepository,
            UserRepository userRepository, BlockPolicyService blockPolicyService) {
        this(conversationRepository, messageRepository, receiptRepository, userRepository, Clock.systemUTC(), blockPolicyService);
    }

    DirectMessagingService(
            DirectConversationRepository conversationRepository,
            DirectMessageRepository messageRepository,
            MessageReadReceiptRepository receiptRepository,
            UserRepository userRepository,
            Clock clock) {
        this(conversationRepository, messageRepository, receiptRepository, userRepository, clock, null);
    }

    DirectMessagingService(DirectConversationRepository conversationRepository,
            DirectMessageRepository messageRepository, MessageReadReceiptRepository receiptRepository,
            UserRepository userRepository, Clock clock, BlockPolicyService blockPolicyService) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.receiptRepository = receiptRepository;
        this.userRepository = userRepository;
        this.clock = clock;
        this.blockPolicyService = blockPolicyService;
    }

    @Transactional
    public DirectConversationResponse startConversation(UUID actingUserId, String recipientUsername) {
        User actingUser = requireUser(actingUserId);
        String normalizedUsername = normalizeUsername(recipientUsername);
        User recipient = userRepository.findByUsernameIgnoreCase(normalizedUsername)
                .orElseThrow(UserNotFoundException::new);
        if (actingUserId.equals(recipient.getId())) {
            throw new InvalidDirectMessageException("You cannot start a direct conversation with yourself");
        }
        requireAllowed(actingUserId, recipient.getId());

        String participantKey = participantKey(actingUserId, recipient.getId());
        DirectConversation conversation = conversationRepository.findByParticipantKey(participantKey)
                .orElseGet(() -> {
                    DirectConversation created = new DirectConversation(participantKey);
                    created.addParticipant(actingUser);
                    created.addParticipant(recipient);
                    return conversationRepository.saveAndFlush(created);
                });
        return toConversationResponse(conversation, actingUserId);
    }

    @Transactional(readOnly = true)
    public List<DirectConversationResponse> getConversations(UUID actingUserId) {
        requireUser(actingUserId);
        return conversationRepository.findAllForUser(actingUserId).stream()
                .filter(conversation -> !isBlocked(actingUserId, otherParticipant(conversation, actingUserId).getId()))
                .map(conversation -> toConversationResponse(conversation, actingUserId))
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<DirectMessageResponse> getHistory(
            UUID actingUserId,
            UUID conversationId,
            Pageable pageable) {
        DirectConversation conversation = requireAccessible(conversationId, actingUserId);
        Page<DirectMessage> messages = messageRepository
                .findAllByConversationIdOrderByCreatedAtDescIdDesc(
                        conversationId, normalize(pageable));
        return PageResponse.from(messages, message -> toMessageResponse(message, conversation));
    }

    @Transactional
    public DirectMessageResponse sendMessage(
            UUID actingUserId,
            UUID conversationId,
            String content) {
        DirectConversation conversation = requireAccessible(conversationId, actingUserId);
        User sender = requireUser(actingUserId);
        DirectMessage message = messageRepository.saveAndFlush(
                new DirectMessage(conversation, sender, normalizeContent(content)));
        conversation.touch();
        return toMessageResponse(message, conversation);
    }

    @Transactional
    public DirectMessageReadResponse markConversationRead(UUID actingUserId, UUID conversationId) {
        requireAccessible(conversationId, actingUserId);
        User reader = requireUser(actingUserId);
        List<DirectMessage> unread = messageRepository.findUnreadForUser(conversationId, actingUserId);
        Instant readAt = clock.instant();
        receiptRepository.saveAll(unread.stream()
                .map(message -> new MessageReadReceipt(message, reader, readAt))
                .toList());
        return new DirectMessageReadResponse(unread.size(), readAt);
    }

    @Transactional
    public DirectMessageResponse deleteMessage(
            UUID actingUserId,
            UUID conversationId,
            UUID messageId) {
        DirectConversation conversation = requireAccessible(conversationId, actingUserId);
        DirectMessage message = messageRepository.findByIdAndConversationId(messageId, conversationId)
                .orElseThrow(DirectMessageNotFoundException::new);
        if (!message.getSender().getId().equals(actingUserId)) {
            throw new UnauthorizedActionException("Only the sender can delete this direct message");
        }
        if (!message.isDeleted()) {
            message.softDelete();
            conversation.touch();
        }
        return toMessageResponse(message, conversation);
    }

    private DirectConversationResponse toConversationResponse(
            DirectConversation conversation,
            UUID actingUserId) {
        User other = otherParticipant(conversation, actingUserId);
        DirectMessage latest = messageRepository
                .findFirstByConversationIdOrderByCreatedAtDescIdDesc(conversation.getId())
                .orElse(null);
        String preview = latest == null ? null : latest.isDeleted() ? "Message deleted" : preview(latest.getContent());
        Instant lastMessageAt = latest == null ? null : latest.getCreatedAt();
        return new DirectConversationResponse(
                conversation.getId(),
                PostAuthorResponse.from(other),
                preview,
                lastMessageAt,
                messageRepository.countUnreadForUser(conversation.getId(), actingUserId),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt());
    }

    private DirectMessageResponse toMessageResponse(
            DirectMessage message,
            DirectConversation conversation) {
        UUID recipientId = otherParticipant(conversation, message.getSender().getId()).getId();
        boolean read = receiptRepository.existsByMessageIdAndUserId(message.getId(), recipientId);
        return DirectMessageResponse.from(message, read);
    }

    private DirectConversation requireAccessible(UUID conversationId, UUID userId) {
        DirectConversation conversation = conversationRepository.findAccessible(conversationId, userId)
                .orElseThrow(DirectConversationNotFoundException::new);
        requireAllowed(userId, otherParticipant(conversation, userId).getId());
        return conversation;
    }

    private void requireAllowed(UUID firstId, UUID secondId) {
        if (blockPolicyService != null) blockPolicyService.requireInteractionAllowed(firstId, secondId);
    }

    private boolean isBlocked(UUID firstId, UUID secondId) {
        return blockPolicyService != null && blockPolicyService.isBlockedEitherDirection(firstId, secondId);
    }

    private User requireUser(UUID userId) {
        return userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
    }

    private User otherParticipant(DirectConversation conversation, UUID actingUserId) {
        return conversation.getParticipants().stream()
                .map(ConversationParticipant::getUser)
                .filter(user -> !user.getId().equals(actingUserId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Direct conversation must have two participants"));
    }

    private String normalizeUsername(String username) {
        String normalized = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("@")) normalized = normalized.substring(1);
        if (normalized.isBlank()) throw new InvalidDirectMessageException("Recipient username is required");
        return normalized;
    }

    private String normalizeContent(String content) {
        String normalized = content == null ? "" : content.trim();
        if (normalized.isEmpty()) throw new InvalidDirectMessageException("Message content is required");
        if (normalized.codePointCount(0, normalized.length()) > MAX_MESSAGE_LENGTH) {
            throw new InvalidDirectMessageException(
                    "Message content must not exceed " + MAX_MESSAGE_LENGTH + " characters");
        }
        return normalized;
    }

    private String preview(String content) {
        String normalized = content.replaceAll("\\s+", " ");
        return normalized.length() <= 90 ? normalized : normalized.substring(0, 89).trim() + "…";
    }

    private String participantKey(UUID first, UUID second) {
        return List.of(first, second).stream()
                .sorted(Comparator.comparing(UUID::toString))
                .map(UUID::toString)
                .reduce((left, right) -> left + ":" + right)
                .orElseThrow();
    }

    private Pageable normalize(Pageable pageable) {
        int page = Math.max(0, pageable.getPageNumber());
        int size = Math.max(1, Math.min(pageable.getPageSize(), MAX_PAGE_SIZE));
        return PageRequest.of(page, size);
    }
}
