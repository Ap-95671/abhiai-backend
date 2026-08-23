package com.abhiai.abhiai_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.context.ApplicationEventPublisher;

import com.abhiai.abhiai_backend.config.MediaProperties;
import com.abhiai.abhiai_backend.dto.media.MediaKind;
import com.abhiai.abhiai_backend.entity.MediaAsset;
import com.abhiai.abhiai_backend.entity.User;
import com.abhiai.abhiai_backend.exception.InvalidMediaException;
import com.abhiai.abhiai_backend.repository.MediaAssetRepository;
import com.abhiai.abhiai_backend.repository.UserRepository;
import com.abhiai.abhiai_backend.repository.StoryRepository;

@ExtendWith(MockitoExtension.class)
class MediaServiceTest {
    private static final UUID USER_ID = UUID.randomUUID();
    @Mock private MediaAssetRepository repository;
    @Mock private UserRepository users;
    @Mock private MediaStorage storage;
    @Mock private PostAccessService postAccess;
    @Mock private ApplicationEventPublisher events;
    @Mock private StoryRepository stories;
    private MediaService service;

    @BeforeEach
    void setUp() {
        service = new MediaService(repository, users, storage, new MediaProperties(), postAccess, events, stories);
    }

    @Test
    void storesAValidPdfAsADocument() {
        User user = new User("tester", "Tester", "tester@example.com", "hash");
        MockMultipartFile file = new MockMultipartFile("file", "design.pdf", "application/pdf", "%PDF-1.7\ncontent".getBytes());
        when(users.findById(USER_ID)).thenReturn(Optional.of(user));
        when(repository.saveAndFlush(any(MediaAsset.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.uploadAttachment(USER_ID, file);

        assertEquals(MediaKind.DOCUMENT, response.kind());
        assertEquals("application/pdf", response.contentType());
        verify(storage).store(any(), any(), any(Long.class), any());
    }

    @Test
    void rejectsAFileWhoseBytesDoNotMatchItsDeclaredType() {
        MockMultipartFile file = new MockMultipartFile("file", "fake.pdf", "application/pdf", "not a pdf".getBytes());

        assertThrows(InvalidMediaException.class, () -> service.uploadAttachment(USER_ID, file));

        verify(storage, never()).store(any(), any(), any(Long.class), any());
        verify(repository, never()).saveAndFlush(any());
    }
}
