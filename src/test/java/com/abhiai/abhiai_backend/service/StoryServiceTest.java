package com.abhiai.abhiai_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.abhiai.abhiai_backend.config.StoryProperties;
import com.abhiai.abhiai_backend.dto.story.CreateStoryRequest;
import com.abhiai.abhiai_backend.entity.MediaAsset;
import com.abhiai.abhiai_backend.entity.Story;
import com.abhiai.abhiai_backend.entity.StoryReaction;
import com.abhiai.abhiai_backend.entity.StoryType;
import com.abhiai.abhiai_backend.entity.User;
import com.abhiai.abhiai_backend.exception.InvalidStoryException;
import com.abhiai.abhiai_backend.exception.StoryNotFoundException;
import com.abhiai.abhiai_backend.repository.MediaAssetRepository;
import com.abhiai.abhiai_backend.repository.StoryReactionRepository;
import com.abhiai.abhiai_backend.repository.StoryRepository;
import com.abhiai.abhiai_backend.repository.StoryViewRepository;
import com.abhiai.abhiai_backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class StoryServiceTest {

    private static final UUID AUTHOR_ID = UUID.randomUUID();
    private static final UUID VIEWER_ID = UUID.randomUUID();
    private static final UUID STORY_ID = UUID.randomUUID();

    @Mock private StoryRepository storyRepository;
    @Mock private StoryViewRepository viewRepository;
    @Mock private StoryReactionRepository reactionRepository;
    @Mock private MediaAssetRepository mediaRepository;
    @Mock private UserRepository userRepository;

    private StoryService storyService;
    private User author;

    @BeforeEach
    void setUp() {
        StoryProperties properties = new StoryProperties();
        properties.setLifetime(Duration.ofHours(24));
        storyService = new StoryService(
                storyRepository, viewRepository, reactionRepository,
                mediaRepository, userRepository, properties);
        author = user(AUTHOR_ID, "author");
    }

    @Test
    void createsATextStoryWithA24HourExpiration() {
        when(userRepository.findById(AUTHOR_ID)).thenReturn(Optional.of(author));
        when(storyRepository.saveAndFlush(any(Story.class))).thenAnswer(invocation -> {
            Story story = invocation.getArgument(0);
            ReflectionTestUtils.setField(story, "id", STORY_ID);
            return story;
        });

        var response = storyService.create(AUTHOR_ID, new CreateStoryRequest("  Building AbhiAI  ", null, null));

        assertEquals(StoryType.TEXT, response.type());
        assertEquals("Building AbhiAI", response.textContent());
        assertEquals("#263B80", response.backgroundColor());
        assertNotNull(response.expiresAt());
        assertTrue(response.expiresAt().isAfter(Instant.now().plus(Duration.ofHours(23))));
    }

    @Test
    void createsAnImageStoryFromOwnedUnusedMedia() {
        UUID mediaId = UUID.randomUUID();
        MediaAsset image = new MediaAsset(mediaId, author, "story.jpg", "story.jpg", "image/jpeg", 1024);
        when(userRepository.findById(AUTHOR_ID)).thenReturn(Optional.of(author));
        when(mediaRepository.findByIdAndOwnerIdAndPostIsNull(mediaId, AUTHOR_ID)).thenReturn(Optional.of(image));
        when(storyRepository.existsByMediaId(mediaId)).thenReturn(false);
        when(userRepository.isUsedAsProfileMedia(mediaId)).thenReturn(false);
        when(storyRepository.saveAndFlush(any(Story.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = storyService.create(AUTHOR_ID, new CreateStoryRequest("Caption", mediaId, "#112233"));

        assertEquals(StoryType.IMAGE, response.type());
        assertEquals(mediaId, response.media().id());
        assertEquals("#112233", response.backgroundColor());
    }

    @Test
    void rejectsAnEmptyStory() {
        when(userRepository.findById(AUTHOR_ID)).thenReturn(Optional.of(author));

        assertThrows(InvalidStoryException.class,
                () -> storyService.create(AUTHOR_ID, new CreateStoryRequest("  ", null, null)));

        verify(storyRepository, never()).saveAndFlush(any());
    }

    @Test
    void recordsAViewOnlyOnceAndDoesNotCountTheAuthor() {
        Story story = story();
        when(userRepository.existsById(VIEWER_ID)).thenReturn(true);
        when(storyRepository.findByIdAndExpiresAtAfter(eq(STORY_ID), any(Instant.class))).thenReturn(Optional.of(story));
        when(viewRepository.insertIfAbsent(any(UUID.class), eq(STORY_ID), eq(VIEWER_ID))).thenReturn(1);

        var response = storyService.recordView(VIEWER_ID, STORY_ID);

        assertTrue(response.counted());
        assertEquals(1, response.viewCount());
        verify(storyRepository).incrementViewCount(STORY_ID);

        when(userRepository.existsById(AUTHOR_ID)).thenReturn(true);
        var authorResponse = storyService.recordView(AUTHOR_ID, STORY_ID);
        assertFalse(authorResponse.counted());
        verify(viewRepository, never()).insertIfAbsent(any(UUID.class), eq(STORY_ID), eq(AUTHOR_ID));
    }

    @Test
    void addsAndUpdatesAReactionWithoutInflatingTheCount() {
        Story story = story();
        when(userRepository.findById(VIEWER_ID)).thenReturn(Optional.of(user(VIEWER_ID, "viewer")));
        when(storyRepository.findByIdAndExpiresAtAfter(eq(STORY_ID), any(Instant.class))).thenReturn(Optional.of(story));
        when(reactionRepository.findByStoryIdAndUserId(STORY_ID, VIEWER_ID))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(new StoryReaction(story, user(VIEWER_ID, "viewer"), "❤️")));

        var created = storyService.react(VIEWER_ID, STORY_ID, "❤️");
        var updated = storyService.react(VIEWER_ID, STORY_ID, "🔥");

        assertEquals(1, created.reactionCount());
        assertEquals("🔥", updated.reaction());
        verify(storyRepository).incrementReactionCount(STORY_ID);
    }

    @Test
    void rejectsUnsupportedReactions() {
        Story story = story();
        when(userRepository.findById(VIEWER_ID)).thenReturn(Optional.of(user(VIEWER_ID, "viewer")));
        when(storyRepository.findByIdAndExpiresAtAfter(eq(STORY_ID), any(Instant.class))).thenReturn(Optional.of(story));

        assertThrows(InvalidStoryException.class,
                () -> storyService.react(VIEWER_ID, STORY_ID, "not-an-emoji"));

        verify(reactionRepository, never()).saveAndFlush(any());
    }

    @Test
    void expiredStoriesAreNotAccessible() {
        when(userRepository.existsById(VIEWER_ID)).thenReturn(true);
        when(storyRepository.findByIdAndExpiresAtAfter(eq(STORY_ID), any(Instant.class))).thenReturn(Optional.empty());

        assertThrows(StoryNotFoundException.class, () -> storyService.get(VIEWER_ID, STORY_ID));
    }

    private Story story() {
        Story story = new Story(author, null, StoryType.TEXT, "Story", "#263B80", Instant.now().plusSeconds(3600));
        ReflectionTestUtils.setField(story, "id", STORY_ID);
        return story;
    }

    private User user(UUID id, String username) {
        User user = new User(username, "Display", username + "@example.com", "hash");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
