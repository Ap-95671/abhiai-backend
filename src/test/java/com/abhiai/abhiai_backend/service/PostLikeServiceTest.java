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
import java.util.Optional;
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
import com.abhiai.abhiai_backend.dto.like.PostLikeResponse;
import com.abhiai.abhiai_backend.dto.like.PostLikeStatusResponse;
import com.abhiai.abhiai_backend.dto.like.PostLikeUserResponse;
import com.abhiai.abhiai_backend.entity.Post;
import com.abhiai.abhiai_backend.entity.PostLike;
import com.abhiai.abhiai_backend.entity.NotificationType;
import com.abhiai.abhiai_backend.entity.PostVisibility;
import com.abhiai.abhiai_backend.entity.User;
import com.abhiai.abhiai_backend.exception.DuplicatePostLikeException;
import com.abhiai.abhiai_backend.exception.PostLikeNotFoundException;
import com.abhiai.abhiai_backend.repository.PostLikeRepository;
import com.abhiai.abhiai_backend.repository.PostRepository;
import com.abhiai.abhiai_backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class PostLikeServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID POST_ID = UUID.randomUUID();

    @Mock
    private PostLikeRepository postLikeRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PostAccessService postAccessService;

    @Mock
    private SocialNotificationService notificationService;
    @Mock private CreatorAnalyticsService analyticsService;

    private PostLikeService postLikeService;
    private User user;
    private Post post;

    @BeforeEach
    void setUp() {
        postLikeService = new PostLikeService(
                postLikeRepository,
                postRepository,
                userRepository,
                postAccessService,
                notificationService,
                analyticsService);
        user = user(USER_ID, "liker");
        post = new Post(user, "Post", PostVisibility.PUBLIC);
        ReflectionTestUtils.setField(post, "id", POST_ID);
    }

    @Test
    void likesAVisiblePostAndIncrementsItsCounter() {
        when(postAccessService.findViewablePost(USER_ID, POST_ID)).thenReturn(post);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(postLikeRepository.existsByPostIdAndUserId(POST_ID, USER_ID)).thenReturn(false);
        when(postLikeRepository.saveAndFlush(any(PostLike.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PostLikeResponse response = postLikeService.like(USER_ID, POST_ID);

        assertEquals(POST_ID, response.postId());
        assertEquals(USER_ID, response.userId());
        assertTrue(response.liked());
        verify(postRepository).incrementLikeCount(POST_ID);
        verify(notificationService).notifyPostInteraction(NotificationType.POST_LIKE, user, post);
    }

    @Test
    void rejectsDuplicateLikesBeforeWriting() {
        when(postAccessService.findViewablePost(USER_ID, POST_ID)).thenReturn(post);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(postLikeRepository.existsByPostIdAndUserId(POST_ID, USER_ID)).thenReturn(true);

        assertThrows(DuplicatePostLikeException.class, () -> postLikeService.like(USER_ID, POST_ID));

        verify(postLikeRepository, never()).saveAndFlush(any());
        verify(postRepository, never()).incrementLikeCount(any());
        verify(notificationService, never()).notifyPostInteraction(any(), any(), any());
    }

    @Test
    void unlikesAnActivePostAndDecrementsItsCounter() {
        PostLike like = new PostLike(post, user);
        when(postAccessService.findActivePost(POST_ID)).thenReturn(post);
        when(postLikeRepository.findByPostIdAndUserId(POST_ID, USER_ID))
                .thenReturn(Optional.of(like));

        postLikeService.unlike(USER_ID, POST_ID);

        verify(postLikeRepository).delete(like);
        verify(postLikeRepository).flush();
        verify(postRepository).decrementLikeCount(POST_ID);
    }

    @Test
    void reportsAMissingLike() {
        when(postAccessService.findActivePost(POST_ID)).thenReturn(post);
        when(postLikeRepository.findByPostIdAndUserId(POST_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertThrows(PostLikeNotFoundException.class, () -> postLikeService.unlike(USER_ID, POST_ID));
        verify(postRepository, never()).decrementLikeCount(any());
    }

    @Test
    void returnsTheCurrentUsersLikeStatus() {
        when(postAccessService.findViewablePost(USER_ID, POST_ID)).thenReturn(post);
        when(postLikeRepository.existsByPostIdAndUserId(POST_ID, USER_ID))
                .thenReturn(true, false);

        PostLikeStatusResponse liked = postLikeService.getStatus(USER_ID, POST_ID);
        PostLikeStatusResponse notLiked = postLikeService.getStatus(USER_ID, POST_ID);

        assertTrue(liked.liked());
        assertFalse(notLiked.liked());
    }

    @Test
    void listsLikesWithBoundedStablePagination() {
        PostLike like = new PostLike(post, user);
        when(postAccessService.findViewablePost(USER_ID, POST_ID)).thenReturn(post);
        when(postLikeRepository.findByPostId(eq(POST_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(like), PageRequest.of(0, 100), 1));

        PageResponse<PostLikeUserResponse> response = postLikeService.getLikes(
                USER_ID,
                POST_ID,
                PageRequest.of(0, 500));

        assertEquals(USER_ID, response.content().getFirst().id());
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(postLikeRepository).findByPostId(eq(POST_ID), pageableCaptor.capture());
        assertEquals(100, pageableCaptor.getValue().getPageSize());
        assertEquals("createdAt: DESC,id: DESC", pageableCaptor.getValue().getSort().toString());
    }

    private User user(UUID id, String username) {
        User result = new User(username, "Display", username + "@example.com", "hash");
        ReflectionTestUtils.setField(result, "id", id);
        return result;
    }
}
