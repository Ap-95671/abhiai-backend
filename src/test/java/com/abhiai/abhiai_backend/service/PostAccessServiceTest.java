package com.abhiai.abhiai_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.abhiai.abhiai_backend.entity.Post;
import com.abhiai.abhiai_backend.entity.PostVisibility;
import com.abhiai.abhiai_backend.entity.User;
import com.abhiai.abhiai_backend.exception.PostNotFoundException;
import com.abhiai.abhiai_backend.exception.UnauthorizedActionException;
import com.abhiai.abhiai_backend.repository.FollowRepository;
import com.abhiai.abhiai_backend.repository.PostRepository;

@ExtendWith(MockitoExtension.class)
class PostAccessServiceTest {

    private static final UUID AUTHOR_ID = UUID.randomUUID();
    private static final UUID VIEWER_ID = UUID.randomUUID();
    private static final UUID POST_ID = UUID.randomUUID();

    @Mock
    private PostRepository postRepository;

    @Mock
    private FollowRepository followRepository;

    private PostAccessService postAccessService;

    @BeforeEach
    void setUp() {
        postAccessService = new PostAccessService(postRepository, followRepository);
    }

    @Test
    void allowsPublicPostsAndTheirAuthors() {
        Post publicPost = post(PostVisibility.PUBLIC);
        when(postRepository.findByIdAndDeletedAtIsNull(POST_ID)).thenReturn(Optional.of(publicPost));

        assertEquals(publicPost, postAccessService.findViewablePost(VIEWER_ID, POST_ID));
        assertEquals(publicPost, postAccessService.findViewablePost(AUTHOR_ID, POST_ID));
        verifyNoInteractions(followRepository);
    }

    @Test
    void allowsFollowerOnlyPostsOnlyToFollowers() {
        Post followersPost = post(PostVisibility.FOLLOWERS);
        when(postRepository.findByIdAndDeletedAtIsNull(POST_ID)).thenReturn(Optional.of(followersPost));
        when(followRepository.existsByFollowerIdAndFollowingId(VIEWER_ID, AUTHOR_ID))
                .thenReturn(true, false);

        assertEquals(followersPost, postAccessService.findViewablePost(VIEWER_ID, POST_ID));
        assertThrows(
                UnauthorizedActionException.class,
                () -> postAccessService.findViewablePost(VIEWER_ID, POST_ID));
    }

    @Test
    void restrictsPrivatePostsToTheirAuthor() {
        Post privatePost = post(PostVisibility.PRIVATE);
        when(postRepository.findByIdAndDeletedAtIsNull(POST_ID)).thenReturn(Optional.of(privatePost));

        assertEquals(privatePost, postAccessService.findViewablePost(AUTHOR_ID, POST_ID));
        assertThrows(
                UnauthorizedActionException.class,
                () -> postAccessService.findViewablePost(VIEWER_ID, POST_ID));
        verifyNoInteractions(followRepository);
    }

    @Test
    void treatsDeletedOrUnknownPostsAsNotFound() {
        when(postRepository.findByIdAndDeletedAtIsNull(POST_ID)).thenReturn(Optional.empty());

        assertThrows(PostNotFoundException.class, () -> postAccessService.findActivePost(POST_ID));
    }

    private Post post(PostVisibility visibility) {
        User author = new User("author", "Author", "author@example.com", "hash");
        ReflectionTestUtils.setField(author, "id", AUTHOR_ID);
        Post post = new Post(author, "Post", visibility);
        ReflectionTestUtils.setField(post, "id", POST_ID);
        return post;
    }
}
