package com.abhiai.abhiai_backend.assistant;

import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.abhiai.abhiai_backend.dto.chat.*;
import com.abhiai.abhiai_backend.entity.*;
import com.abhiai.abhiai_backend.repository.*;
import com.abhiai.abhiai_backend.exception.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

@Service
public class AssistantConversationService {
    public record TranscriptItem(@NotBlank @Size(max = 128) @Pattern(regexp = "[A-Za-z0-9_-]+") String id,
                                 @NotNull MessageRole role, @NotBlank @Size(max = 10000) String content) {}
    public record TranscriptBatch(@NotEmpty @Size(max = 40) List<@Valid TranscriptItem> messages) {}
    private final ConversationRepository conversations;
    private final MessageRepository messages;
    private final UserRepository users;
    public AssistantConversationService(ConversationRepository conversations, MessageRepository messages, UserRepository users) {
        this.conversations = conversations; this.messages = messages; this.users = users;
    }
    @Transactional
    public ConversationDetailResponse open(UUID userId, boolean fresh) {
        // Lock the user row so simultaneous tabs cannot create two initial assistant conversations.
        User user = users.lockAssistantOwner(userId).orElseThrow(InvalidCredentialsException::new);
        Conversation conversation = fresh ? null : conversations
                .findFirstByUserIdAndCharacterAssistantTrueOrderByUpdatedAtDesc(userId).orElse(null);
        if (conversation == null) {
            conversation = new Conversation(user, "AbhiAI Assistant");
            conversation.markCharacterAssistant();
            conversation = conversations.saveAndFlush(conversation);
        }
        return detail(conversation);
    }
    @Transactional(readOnly = true)
    public ConversationDetailResponse history(UUID userId, UUID id) { return detail(owned(userId, id)); }
    private Conversation owned(UUID userId, UUID id) {
        return conversations.findByIdAndUserId(id, userId).filter(Conversation::isCharacterAssistant)
                .orElseThrow(ConversationNotFoundException::new);
    }
    private ConversationDetailResponse detail(Conversation c) {
        return ConversationDetailResponse.from(c, messages.findAllByConversationIdOrderByCreatedAtAscIdAsc(c.getId())
                .stream().map(MessageResponse::from).toList());
    }
    @Transactional
    public void append(UUID userId, UUID id, TranscriptBatch batch) {
        Conversation c = conversations.lockAssistant(id, userId).orElseThrow(ConversationNotFoundException::new);
        Set<String> saved = new HashSet<>();
        messages.findAllByConversationIdOrderByCreatedAtAscIdAsc(id).forEach(m -> saved.add(m.getClientItemId()));
        // Transcripts are untrusted user-owned history, never SYSTEM instructions or tool calls.
        for (TranscriptItem item : batch.messages()) {
            if (item.role() != MessageRole.USER && item.role() != MessageRole.ASSISTANT)
                throw new AssistantException(org.springframework.http.HttpStatus.BAD_REQUEST, "Unsupported transcript role.");
            if (saved.add(item.id())) {
                Message message = new Message(c, item.role(), item.content());
                message.setClientItemId(item.id());
                messages.saveAndFlush(message);
            }
        }
        c.touch();
    }
}
