package com.abhiai.abhiai_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import com.abhiai.abhiai_backend.dto.common.PageResponse;
import com.abhiai.abhiai_backend.dto.post.PostResponse;
import com.abhiai.abhiai_backend.dto.post.PostViewResponse;
import com.abhiai.abhiai_backend.entity.MediaAsset;
import com.abhiai.abhiai_backend.entity.Post;
import com.abhiai.abhiai_backend.entity.PostVisibility;
import com.abhiai.abhiai_backend.entity.User;
import com.abhiai.abhiai_backend.exception.InvalidPostException;
import com.abhiai.abhiai_backend.repository.PostRepository;
import com.abhiai.abhiai_backend.repository.PostViewRepository;
import com.abhiai.abhiai_backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class VideoFeedServiceTest {

    private static final UUID AUTHOR_ID = UUID.randomUUID();
    private static final UUID VIEWER_ID = UUID.randomUUID();
    private static final UUID POST_ID = UUID.randomUUID();

    @Mock private PostRepository postRepository;
    @Mock private PostViewRepository postViewRepository;
    @Mock private PostAccessService postAccessService;
    @Mock private UserRepository userRepository;

    private VideoFeedService videoFeedService;
    private User author;

    @BeforeEach
    void setUp() {
        videoFeedService = new VideoFeedService(
                postRepository, postViewRepository, postAccessService, userRepository);
        author = user(AUTHOR_ID, "creator");
    }

    @Test
    void returnsVideoPostsWithBoundedChronologicalPagination() {
        Post videoPost = videoPost();
        when(userRepository.existsById(VIEWER_ID)).thenReturn(true);
        when(postRepository.findVideoFeed(eq(VIEWER_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(videoPost), PageRequest.of(2, 50), 101));

        PageResponse<PostResponse> response = videoFeedService.getVideoFeed(
                VIEWER_ID, PageRequest.of(2, 500));

        assertEquals(POST_ID, response.content().getFirst().id());
        assertEquals("VIDEO", response.content().getFirst().media().getFirst().kind().name());
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(postRepository).findVideoFeed(eq(VIEWER_ID), pageable.capture());
        assertEquals(50, pageable.getValue().getPageSize());
        assertEquals("createdAt: DESC,id: DESC", pageable.getValue().getSort().toString());
    }

    @Test
    void recordsAUniqueViewerAndIncrementsTheCounterOnce() {
        Post videoPost = videoPost();
        ReflectionTestUtils.setField(videoPost, "viewCount", 7L);
        when(userRepository.existsById(VIEWER_ID)).thenReturn(true);
        when(postAccessService.findViewablePost(VIEWER_ID, POST_ID)).thenReturn(videoPost);
        when(postViewRepository.insertIfAbsent(any(UUID.class), eq(POST_ID), eq(VIEWER_ID)))
                .thenReturn(1);

        PostViewResponse response = videoFeedService.recordView(VIEWER_ID, POST_ID);

        assertTrue(response.counted());
        assertEquals(8, response.viewCount());
        verify(postRepository).incrementViewCount(POST_ID);
    }

    @Test
    void treatsARepeatedViewAsIdempotent() {
        Post videoPost = videoPost();
        ReflectionTestUtils.setField(videoPost, "viewCount", 8L);
        when(userRepository.existsById(VIEWER_ID)).thenReturn(true);
        when(postAccessService.findViewablePost(VIEWER_ID, POST_ID)).thenReturn(videoPost);
        when(postViewRepository.insertIfAbsent(any(UUID.class), eq(POST_ID), eq(VIEWER_ID)))
                .thenReturn(0);

        PostViewResponse response = videoFeedService.recordView(VIEWER_ID, POST_ID);

        assertFalse(response.counted());
        assertEquals(8, response.viewCount());
        verify(postRepository, never()).incrementViewCount(any());
    }

    @Test
    void doesNotCountTheCreatorsOwnPlayback() {
        Post videoPost = videoPost();
        when(userRepository.existsById(AUTHOR_ID)).thenReturn(true);
        when(postAccessService.findViewablePost(AUTHOR_ID, POST_ID)).thenReturn(videoPost);

        PostViewResponse response = videoFeedService.recordView(AUTHOR_ID, POST_ID);

        assertFalse(response.counted());
        verify(postViewRepository, never()).insertIfAbsent(any(), any(), any());
        verify(postRepository, never()).incrementViewCount(any());
    }

    @Test
    void rejectsViewEventsForNonVideoPosts() {
        Post textPost = new Post(author, "Text only", PostVisibility.PUBLIC);
        ReflectionTestUtils.setField(textPost, "id", POST_ID);
        when(userRepository.existsById(VIEWER_ID)).thenReturn(true);
        when(postAccessService.findViewablePost(VIEWER_ID, POST_ID)).thenReturn(textPost);

        assertThrows(InvalidPostException.class,
                () -> videoFeedService.recordView(VIEWER_ID, POST_ID));

        verify(postViewRepository, never()).insertIfAbsent(any(), any(), any());
    }

    private Post videoPost() {
        Post post = new Post(author, "A short video", PostVisibility.PUBLIC);
        ReflectionTestUtils.setField(post, "id", POST_ID);
        MediaAsset video = new MediaAsset(
                UUID.randomUUID(), author, "video.mp4", "clip.mp4", "video/mp4", 1024);
        post.attachMedia(List.of(video));
        return post;
    }

    private User user(UUID id, String username) {
        User user = new User(username, "Creator", username + "@example.com", "hash");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
