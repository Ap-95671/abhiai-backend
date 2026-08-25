package com.abhiai.abhiai_backend.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.abhiai.abhiai_backend.ai.image.ImageGenerationProvider;
import com.abhiai.abhiai_backend.dto.chat.ChatExchangeResponse;
import com.abhiai.abhiai_backend.dto.chat.ConversationAttachmentResponse;
import com.abhiai.abhiai_backend.dto.chat.ConversationSummaryResponse;
import com.abhiai.abhiai_backend.dto.chat.MessageResponse;
import com.abhiai.abhiai_backend.entity.AiAttachmentKind;
import com.abhiai.abhiai_backend.entity.ConversationAttachment;
import com.abhiai.abhiai_backend.entity.Message;
import com.abhiai.abhiai_backend.entity.MessageRole;
import com.abhiai.abhiai_backend.exception.ConversationNotFoundException;
import com.abhiai.abhiai_backend.repository.ConversationAttachmentRepository;
import com.abhiai.abhiai_backend.repository.ConversationRepository;
import com.abhiai.abhiai_backend.repository.MessageRepository;

@Service
public class ImageGenerationService {

    private static final String DEFAULT_TITLE = "New conversation";
    private static final int TITLE_MAX_LENGTH = 60;

    private final ConversationRepository conversations;
    private final MessageRepository messages;
    private final ConversationAttachmentRepository attachments;
    private final MediaService mediaService;
    private final ImageGenerationProvider provider;

    public ImageGenerationService(
            ConversationRepository conversations,
            MessageRepository messages,
            ConversationAttachmentRepository attachments,
            MediaService mediaService,
            ImageGenerationProvider provider) {
        this.conversations = conversations;
        this.messages = messages;
        this.attachments = attachments;
        this.mediaService = mediaService;
        this.provider = provider;
    }

    @Transactional
    public ChatExchangeResponse generate(UUID userId, UUID conversationId, String rawPrompt) {
        var conversation = conversations.findByIdAndUserId(conversationId, userId)
                .orElseThrow(ConversationNotFoundException::new);
        String prompt = rawPrompt.trim();
        List<Message> history = messages.findAllByConversationIdOrderByCreatedAtAscIdAsc(conversationId);
        if (history.isEmpty() && DEFAULT_TITLE.equals(conversation.getTitle())) {
            String title = prompt.replaceAll("\\s+", " ");
            conversation.rename(title.length() >= TITLE_MAX_LENGTH
                    ? title.substring(0, TITLE_MAX_LENGTH - 1).trim() + "…"
                    : title);
        }

        Message userMessage = messages.save(new Message(conversation, MessageRole.USER, prompt));
        var generated = provider.generate(prompt);
        Message assistantMessage = messages.save(new Message(
                conversation,
                MessageRole.ASSISTANT,
                "Generated an image from your description."));
        var media = mediaService.storeGeneratedImage(userId, generated.content(), generated.contentType());
        ConversationAttachment attachment = new ConversationAttachment(
                UUID.randomUUID(), conversation, media, AiAttachmentKind.IMAGE);
        attachment.attachToMessage(assistantMessage);
        attachments.save(attachment);
        conversation.touch();

        ConversationAttachmentResponse attachmentResponse = ConversationAttachmentResponse.from(attachment);
        return new ChatExchangeResponse(
                MessageResponse.from(userMessage),
                MessageResponse.from(assistantMessage, List.of(attachmentResponse)),
                ConversationSummaryResponse.from(conversation));
    }

    public boolean configured() { return provider.configured(); }
    public String providerName() { return provider.providerName(); }
    public String modelName() { return provider.modelName(); }
}
