package com.abhiai.abhiai_backend.service;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.abhiai.abhiai_backend.dto.chat.ConversationAttachmentResponse;
import com.abhiai.abhiai_backend.ai.AiInputAttachment;
import com.abhiai.abhiai_backend.entity.AiAttachmentKind;
import com.abhiai.abhiai_backend.entity.AiAttachmentStatus;
import com.abhiai.abhiai_backend.entity.ConversationAttachment;
import com.abhiai.abhiai_backend.entity.MediaAsset;
import com.abhiai.abhiai_backend.entity.Message;
import com.abhiai.abhiai_backend.exception.ConversationNotFoundException;
import com.abhiai.abhiai_backend.exception.InvalidMediaException;
import com.abhiai.abhiai_backend.repository.ConversationAttachmentRepository;
import com.abhiai.abhiai_backend.repository.ConversationRepository;
import com.abhiai.abhiai_backend.repository.MediaAssetRepository;

@Service
public class ConversationAttachmentService {

    private static final Set<String> SUPPORTED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp", "application/pdf");

    private final ConversationRepository conversations;
    private final ConversationAttachmentRepository attachments;
    private final MediaAssetRepository mediaAssets;
    private final MediaService mediaService;
    private final MediaStorage storage;
    private final DocumentExtractionService documentExtraction;
    private final LocalEmbeddingService embeddings;

    public ConversationAttachmentService(
            ConversationRepository conversations,
            ConversationAttachmentRepository attachments,
            MediaAssetRepository mediaAssets,
            MediaService mediaService,
            MediaStorage storage,
            DocumentExtractionService documentExtraction,
            LocalEmbeddingService embeddings) {
        this.conversations = conversations;
        this.attachments = attachments;
        this.mediaAssets = mediaAssets;
        this.mediaService = mediaService;
        this.storage = storage;
        this.documentExtraction = documentExtraction;
        this.embeddings = embeddings;
    }

    @Transactional
    public ConversationAttachmentResponse upload(UUID userId, UUID conversationId, MultipartFile file) {
        var conversation = conversations.findByIdAndUserId(conversationId, userId)
                .orElseThrow(ConversationNotFoundException::new);
        String contentType = file == null ? null : file.getContentType();
        if (contentType == null || !SUPPORTED_TYPES.contains(contentType.toLowerCase())) {
            throw new InvalidMediaException("AI chat supports JPEG, PNG, GIF, WebP, and PDF attachments");
        }

        var uploaded = mediaService.uploadAttachment(userId, file);
        MediaAsset media = mediaAssets.findByIdAndOwnerIdAndPostIsNull(uploaded.id(), userId)
                .orElseThrow(() -> new InvalidMediaException("Uploaded attachment is unavailable"));
        AiAttachmentKind kind = media.getContentType().equals("application/pdf")
                ? AiAttachmentKind.DOCUMENT
                : AiAttachmentKind.IMAGE;
        ConversationAttachment attachment = attachments.save(
                new ConversationAttachment(UUID.randomUUID(), conversation, media, kind));

        if (kind == AiAttachmentKind.DOCUMENT) {
            processDocument(attachment);
        }
        return ConversationAttachmentResponse.from(attachment);
    }

    @Transactional(readOnly = true)
    public List<ConversationAttachmentResponse> list(UUID userId, UUID conversationId) {
        requireOwnedConversation(userId, conversationId);
        return attachments.findAllByConversationIdOrderByCreatedAtAscIdAsc(conversationId)
                .stream()
                .map(ConversationAttachmentResponse::from)
                .toList();
    }

    @Transactional
    public void delete(UUID userId, UUID conversationId, UUID attachmentId) {
        requireOwnedConversation(userId, conversationId);
        ConversationAttachment attachment = attachments.findById(attachmentId)
                .filter(item -> item.getConversation().getId().equals(conversationId))
                .orElseThrow(() -> new InvalidMediaException("Conversation attachment was not found"));
        if (attachment.getMessage() != null) {
            throw new InvalidMediaException("An attachment already used in a message cannot be deleted separately");
        }
        UUID mediaId = attachment.getMediaAsset().getId();
        attachments.delete(attachment);
        attachments.flush();
        mediaService.deleteUnattached(userId, mediaId);
    }

    @Transactional
    public List<ConversationAttachment> claimForMessage(
            UUID conversationId,
            List<UUID> attachmentIds,
            Message message) {
        if (attachmentIds == null || attachmentIds.isEmpty()) {
            return List.of();
        }
        if (attachmentIds.size() > 5 || Set.copyOf(attachmentIds).size() != attachmentIds.size()) {
            throw new InvalidMediaException("A message may contain at most five unique attachments");
        }
        List<ConversationAttachment> selected = attachments
                .findAllByIdInAndConversationIdAndProcessingStatus(
                        attachmentIds, conversationId, AiAttachmentStatus.READY);
        if (selected.size() != attachmentIds.size()
                || selected.stream().anyMatch(item -> item.getMessage() != null)) {
            throw new InvalidMediaException("Every attachment must be ready, unused, and belong to this conversation");
        }
        selected.forEach(item -> item.attachToMessage(message));
        return selected;
    }

    @Transactional
    public PreparedAiInput prepareForMessage(
            UUID conversationId,
            String prompt,
            List<UUID> attachmentIds,
            boolean externalProcessingAllowed,
            Message message) {
        List<ConversationAttachment> selected = claimForMessage(conversationId, attachmentIds, message);
        if (!selected.isEmpty() && !externalProcessingAllowed) {
            throw new InvalidMediaException(
                    "Confirm external AI processing before sending conversation attachments");
        }

        List<AiInputAttachment> images = new ArrayList<>();
        for (ConversationAttachment attachment : selected) {
            if (attachment.getKind() == AiAttachmentKind.IMAGE) {
                try (var input = storage.load(attachment.getMediaAsset().getStorageKey()).getInputStream()) {
                    images.add(new AiInputAttachment(
                            attachment.getMediaAsset().getOriginalFilename(),
                            attachment.getMediaAsset().getContentType(),
                            input.readAllBytes()));
                } catch (IOException exception) {
                    throw new InvalidMediaException("An attached image could not be read");
                }
            }
        }

        String documentContext = externalProcessingAllowed
                ? retrieveRelevantDocumentText(conversationId, prompt, selected)
                : "";
        String augmentedPrompt = documentContext.isBlank()
                ? prompt
                : prompt + "\n\n[Relevant private conversation documents]\n" + documentContext;
        return new PreparedAiInput(augmentedPrompt, images);
    }

    private String retrieveRelevantDocumentText(
            UUID conversationId,
            String prompt,
            List<ConversationAttachment> selected) {
        Set<UUID> selectedIds = selected.stream().map(ConversationAttachment::getId).collect(java.util.stream.Collectors.toSet());
        Set<String> terms = java.util.Arrays.stream(prompt.toLowerCase(Locale.ROOT).split("[^a-z0-9]+"))
                .filter(term -> term.length() > 2)
                .collect(java.util.stream.Collectors.toSet());

        return attachments.findAllByConversationIdOrderByCreatedAtAscIdAsc(conversationId).stream()
                .filter(item -> item.getKind() == AiAttachmentKind.DOCUMENT)
                .filter(item -> item.getProcessingStatus() == AiAttachmentStatus.READY)
                .filter(item -> item.getExtractedText() != null && !item.getExtractedText().isBlank())
                .sorted(Comparator.comparingDouble((ConversationAttachment item) ->
                        selectedIds.contains(item.getId())
                                ? Double.MAX_VALUE
                                : embeddings.similarity(prompt, item.getEmbedding())
                                    + relevance(item.getExtractedText(), terms) * 0.05)
                        .reversed())
                .limit(3)
                .map(item -> {
                    String text = item.getExtractedText();
                    if (text.length() > 20_000) text = text.substring(0, 20_000);
                    return "Document: " + item.getMediaAsset().getOriginalFilename() + "\n" + text;
                })
                .collect(java.util.stream.Collectors.joining("\n\n"));
    }

    private int relevance(String text, Set<String> terms) {
        String normalized = text.toLowerCase(Locale.ROOT);
        return terms.stream().mapToInt(term -> normalized.contains(term) ? 1 : 0).sum();
    }

    private void processDocument(ConversationAttachment attachment) {
        try (var input = storage.load(attachment.getMediaAsset().getStorageKey()).getInputStream()) {
            String extractedText = documentExtraction.extractPdf(input.readAllBytes());
            attachment.processingCompleted(extractedText, embeddings.embed(extractedText));
        } catch (InvalidMediaException exception) {
            attachment.processingFailed(exception.getMessage());
        } catch (IOException exception) {
            attachment.processingFailed("The document could not be read");
        }
    }

    private void requireOwnedConversation(UUID userId, UUID conversationId) {
        conversations.findByIdAndUserId(conversationId, userId)
                .orElseThrow(ConversationNotFoundException::new);
    }

    public record PreparedAiInput(String prompt, List<AiInputAttachment> images) {
    }
}
