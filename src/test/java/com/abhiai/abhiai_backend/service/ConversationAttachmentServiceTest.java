package com.abhiai.abhiai_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import com.abhiai.abhiai_backend.entity.AiAttachmentStatus;
import com.abhiai.abhiai_backend.entity.Conversation;
import com.abhiai.abhiai_backend.entity.Message;
import com.abhiai.abhiai_backend.exception.ConversationNotFoundException;
import com.abhiai.abhiai_backend.exception.InvalidMediaException;
import com.abhiai.abhiai_backend.repository.ConversationAttachmentRepository;
import com.abhiai.abhiai_backend.repository.ConversationRepository;
import com.abhiai.abhiai_backend.repository.MediaAssetRepository;

@ExtendWith(MockitoExtension.class)
class ConversationAttachmentServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID CONVERSATION_ID = UUID.randomUUID();

    @Mock private ConversationRepository conversations;
    @Mock private ConversationAttachmentRepository attachments;
    @Mock private MediaAssetRepository mediaAssets;
    @Mock private MediaService mediaService;
    @Mock private MediaStorage storage;
    @Mock private DocumentExtractionService documentExtraction;
    @Mock private LocalEmbeddingService embeddings;
    @Mock private Conversation conversation;
    @Mock private Message message;

    private ConversationAttachmentService service;

    @BeforeEach
    void setUp() {
        service = new ConversationAttachmentService(
                conversations,
                attachments,
                mediaAssets,
                mediaService,
                storage,
                documentExtraction,
                embeddings);
    }

    @Test
    void rejectsUploadWhenConversationIsNotOwnedByAuthenticatedUser() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.png", "image/png", new byte[] {(byte) 137, 80, 78, 71});
        when(conversations.findByIdAndUserId(CONVERSATION_ID, USER_ID)).thenReturn(Optional.empty());

        assertThrows(ConversationNotFoundException.class,
                () -> service.upload(USER_ID, CONVERSATION_ID, file));

        verifyNoInteractions(mediaService, mediaAssets, attachments, storage);
    }

    @Test
    void rejectsEmptyUploadBeforeCreatingMedia() {
        MockMultipartFile file = new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]);
        when(conversations.findByIdAndUserId(CONVERSATION_ID, USER_ID)).thenReturn(Optional.of(conversation));

        InvalidMediaException error = assertThrows(InvalidMediaException.class,
                () -> service.upload(USER_ID, CONVERSATION_ID, file));

        assertEquals("Choose a file to upload", error.getMessage());
        verifyNoInteractions(mediaService, mediaAssets, attachments, storage);
    }

    @Test
    void rejectsGifForAiChatEvenThoughGeneralMediaSupportsIt() {
        MockMultipartFile file = new MockMultipartFile("file", "animation.gif", "image/gif", "GIF89a".getBytes());
        when(conversations.findByIdAndUserId(CONVERSATION_ID, USER_ID)).thenReturn(Optional.of(conversation));

        InvalidMediaException error = assertThrows(InvalidMediaException.class,
                () -> service.upload(USER_ID, CONVERSATION_ID, file));

        assertEquals("AI chat supports JPEG, PNG, WebP, PDF, and UTF-8 text attachments", error.getMessage());
        verifyNoInteractions(mediaService, mediaAssets, attachments, storage);
    }

    @Test
    void listDoesNotRevealAttachmentsFromAnUnownedConversation() {
        when(conversations.findByIdAndUserId(CONVERSATION_ID, USER_ID)).thenReturn(Optional.empty());

        assertThrows(ConversationNotFoundException.class,
                () -> service.list(USER_ID, CONVERSATION_ID));

        verifyNoInteractions(attachments);
    }

    @Test
    void claimRejectsIdsThatAreNotReadyUnusedAndInTheConversation() {
        UUID attachmentId = UUID.randomUUID();
        when(attachments.findAllByIdInAndConversationIdAndProcessingStatus(
                List.of(attachmentId), CONVERSATION_ID, AiAttachmentStatus.READY))
                .thenReturn(List.of());

        assertThrows(InvalidMediaException.class,
                () -> service.claimForMessage(CONVERSATION_ID, List.of(attachmentId), message));
    }

    @Test
    void consentIsRequiredBeforeAttachmentsAreClaimedOrLoaded() {
        UUID attachmentId = UUID.randomUUID();

        InvalidMediaException error = assertThrows(InvalidMediaException.class,
                () -> service.prepareForMessage(
                        CONVERSATION_ID,
                        "Describe the image",
                        List.of(attachmentId),
                        false,
                        message));

        assertEquals("Confirm external AI processing before sending conversation attachments", error.getMessage());
        verifyNoInteractions(attachments, storage, documentExtraction, embeddings);
    }
}
