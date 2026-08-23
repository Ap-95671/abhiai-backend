package com.abhiai.abhiai_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.abhiai.abhiai_backend.config.PostProperties;
import com.abhiai.abhiai_backend.dto.post.CreatePostRequest;
import com.abhiai.abhiai_backend.dto.post.PostResponse;
import com.abhiai.abhiai_backend.dto.post.UpdatePostRequest;
import com.abhiai.abhiai_backend.entity.Post;
import com.abhiai.abhiai_backend.entity.PostVisibility;
import com.abhiai.abhiai_backend.entity.User;
import com.abhiai.abhiai_backend.exception.InvalidPostException;
import com.abhiai.abhiai_backend.exception.PostNotFoundException;
import com.abhiai.abhiai_backend.exception.UnauthorizedActionException;
import com.abhiai.abhiai_backend.repository.PostRepository;
import com.abhiai.abhiai_backend.repository.UserRepository;
import com.abhiai.abhiai_backend.repository.MediaAssetRepository;
import com.abhiai.abhiai_backend.repository.PostPollRepository;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    private static final UUID AUTHOR_ID = UUID.randomUUID();
    private static final UUID VIEWER_ID = UUID.randomUUID();
    private static final UUID POST_ID = UUID.randomUUID();

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PostAccessService postAccessService;
    @Mock
    private MediaAssetRepository mediaAssetRepository;
    @Mock
    private HashtagService hashtagService;
    @Mock
    private MentionService mentionService;
    @Mock
    private PostPollRepository postPollRepository;

    private PostService postService;
    private User author;

    @BeforeEach
    void setUp() {
        PostProperties properties = new PostProperties();
        properties.setMaxTextLength(20);
        postService = new PostService(postRepository, userRepository, postAccessService, properties, mediaAssetRepository, hashtagService, mentionService, postPollRepository);
        author = user(AUTHOR_ID, "author");
    }

    @Test
    void createsAPublicPostByDefaultAndIncrementsTheAuthorCounter() {
        when(userRepository.findById(AUTHOR_ID)).thenReturn(Optional.of(author));
        when(postRepository.saveAndFlush(any(Post.class)))
                .thenAnswer(invocation -> {
                    Post post = invocation.getArgument(0);
                    ReflectionTestUtils.setField(post, "id", POST_ID);
                    return post;
                });

        PostResponse response = postService.createPost(
                AUTHOR_ID,
                new CreatePostRequest("  Building AbhiAI  ", null, null, null));

        assertEquals(POST_ID, response.id());
        assertEquals("Building AbhiAI", response.textContent());
        assertEquals(PostVisibility.PUBLIC, response.visibility());
        verify(userRepository).incrementPostCount(AUTHOR_ID);
    }

    @Test
    void rejectsTextLongerThanTheConfiguredLimit() {
        assertThrows(
                InvalidPostException.class,
                () -> postService.createPost(
                        AUTHOR_ID,
                        new CreatePostRequest("This post is over twenty characters", PostVisibility.PUBLIC, null, null)));

        verify(postRepository, never()).saveAndFlush(any());
        verify(userRepository, never()).findById(any());
        verify(userRepository, never()).incrementPostCount(any());
    }

    @Test
    void allowsFollowersToViewFollowerOnlyPosts() {
        Post post = post(PostVisibility.FOLLOWERS);
        when(postAccessService.findViewablePost(VIEWER_ID, POST_ID)).thenReturn(post);

        PostResponse response = postService.getPost(VIEWER_ID, POST_ID);

        assertEquals(POST_ID, response.id());
    }

    @Test
    void rejectsNonFollowersFromFollowerOnlyPosts() {
        Post post = post(PostVisibility.FOLLOWERS);
        when(postAccessService.findViewablePost(VIEWER_ID, POST_ID))
                .thenThrow(new UnauthorizedActionException("You are not allowed to view this post"));

        assertThrows(
                UnauthorizedActionException.class,
                () -> postService.getPost(VIEWER_ID, POST_ID));
    }

    @Test
    void allowsOnlyTheAuthorToViewAPrivatePost() {
        Post post = post(PostVisibility.PRIVATE);
        when(postAccessService.findViewablePost(AUTHOR_ID, POST_ID)).thenReturn(post);
        when(postAccessService.findViewablePost(VIEWER_ID, POST_ID))
                .thenThrow(new UnauthorizedActionException("You are not allowed to view this post"));

        assertEquals(POST_ID, postService.getPost(AUTHOR_ID, POST_ID).id());
        assertThrows(
                UnauthorizedActionException.class,
                () -> postService.getPost(VIEWER_ID, POST_ID));
    }

    @Test
    void preventsAnotherUserFromUpdatingThePost() {
        Post post = post(PostVisibility.PUBLIC);
        when(postAccessService.findActivePost(POST_ID)).thenReturn(post);

        assertThrows(
                UnauthorizedActionException.class,
                () -> postService.updatePost(
                        VIEWER_ID,
                        POST_ID,
                        new UpdatePostRequest("Changed", null)));

        verify(postRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsAnEmptyPatch() {
        Post post = post(PostVisibility.PUBLIC);
        when(postAccessService.findActivePost(POST_ID)).thenReturn(post);

        assertThrows(
                InvalidPostException.class,
                () -> postService.updatePost(
                        AUTHOR_ID,
                        POST_ID,
                        new UpdatePostRequest(null, null)));

        verify(postRepository, never()).saveAndFlush(any());
    }

    @Test
    void updatesAnAuthorsTextAndVisibility() {
        Post post = post(PostVisibility.PUBLIC);
        when(postAccessService.findActivePost(POST_ID)).thenReturn(post);
        when(postRepository.saveAndFlush(post)).thenReturn(post);

        PostResponse response = postService.updatePost(
                AUTHOR_ID,
                POST_ID,
                new UpdatePostRequest("  Updated post  ", PostVisibility.FOLLOWERS));

        assertEquals("Updated post", response.textContent());
        assertEquals(PostVisibility.FOLLOWERS, response.visibility());
    }

    @Test
    void acceptsMultilinePostUpdates() {
        Post post = post(PostVisibility.PUBLIC);
        when(postAccessService.findActivePost(POST_ID)).thenReturn(post);
        when(postRepository.saveAndFlush(post)).thenReturn(post);

        PostResponse response = postService.updatePost(
                AUTHOR_ID,
                POST_ID,
                new UpdatePostRequest("One\nTwo", null));

        assertEquals("One\nTwo", response.textContent());
    }

    @Test
    void softDeletesAnAuthorsPostAndDecrementsTheCounter() {
        Post post = post(PostVisibility.PUBLIC);
        when(postAccessService.findActivePost(POST_ID)).thenReturn(post);
        when(postRepository.saveAndFlush(post)).thenReturn(post);

        postService.deletePost(AUTHOR_ID, POST_ID);

        assertNotNull(post.getDeletedAt());
        verify(postRepository).saveAndFlush(post);
        verify(userRepository).decrementPostCount(AUTHOR_ID);
        verify(postRepository, never()).delete(any());
    }

    @Test
    void treatsDeletedOrUnknownPostsAsNotFound() {
        when(postAccessService.findViewablePost(VIEWER_ID, POST_ID))
                .thenThrow(new PostNotFoundException());

        assertThrows(PostNotFoundException.class, () -> postService.getPost(VIEWER_ID, POST_ID));
    }

    @Test
    void pinsAnAuthorsPostAndReplacesTheirPreviousPin() {
        Post post = post(PostVisibility.PUBLIC);
        when(postAccessService.findActivePost(POST_ID)).thenReturn(post);
        when(postRepository.saveAndFlush(post)).thenReturn(post);

        PostResponse response = postService.pinPost(AUTHOR_ID, POST_ID);

        assertEquals(true, response.pinned());
        verify(postRepository).clearPinnedPostByAuthorId(AUTHOR_ID);
        verify(postRepository).saveAndFlush(post);
    }

    @Test
    void preventsAnotherUserFromPinningOrUnpinningAPost() {
        Post post = post(PostVisibility.PUBLIC);
        when(postAccessService.findActivePost(POST_ID)).thenReturn(post);

        assertThrows(UnauthorizedActionException.class,
                () -> postService.pinPost(VIEWER_ID, POST_ID));
        assertThrows(UnauthorizedActionException.class,
                () -> postService.unpinPost(VIEWER_ID, POST_ID));

        verify(postRepository, never()).clearPinnedPostByAuthorId(any());
        verify(postRepository, never()).saveAndFlush(any());
    }

    private Post post(PostVisibility visibility) {
        Post post = new Post(author, "Original post", visibility);
        ReflectionTestUtils.setField(post, "id", POST_ID);
        return post;
    }

    private User user(UUID id, String username) {
        User user = new User(username, "Display Name", username + "@example.com", "hash");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
