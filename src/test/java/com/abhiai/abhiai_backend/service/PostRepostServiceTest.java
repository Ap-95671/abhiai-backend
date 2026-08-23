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
import com.abhiai.abhiai_backend.dto.repost.PostRepostResponse;
import com.abhiai.abhiai_backend.dto.repost.PostRepostStatusResponse;
import com.abhiai.abhiai_backend.dto.repost.PostRepostUserResponse;
import com.abhiai.abhiai_backend.entity.Post;
import com.abhiai.abhiai_backend.entity.PostRepost;
import com.abhiai.abhiai_backend.entity.NotificationType;
import com.abhiai.abhiai_backend.entity.PostVisibility;
import com.abhiai.abhiai_backend.entity.User;
import com.abhiai.abhiai_backend.exception.DuplicatePostRepostException;
import com.abhiai.abhiai_backend.exception.PostRepostNotFoundException;
import com.abhiai.abhiai_backend.repository.PostRepository;
import com.abhiai.abhiai_backend.repository.PostRepostRepository;
import com.abhiai.abhiai_backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class PostRepostServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID POST_ID = UUID.randomUUID();

    @Mock
    private PostRepostRepository postRepostRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PostAccessService postAccessService;

    @Mock
    private SocialNotificationService notificationService;
    @Mock private CreatorAnalyticsService analyticsService;

    private PostRepostService postRepostService;
    private User user;
    private Post post;

    @BeforeEach
    void setUp() {
        postRepostService = new PostRepostService(
                postRepostRepository,
                postRepository,
                userRepository,
                postAccessService,
                notificationService,
                analyticsService);
        user = user(USER_ID, "reposter");
        post = new Post(user, "Post", PostVisibility.PUBLIC);
        ReflectionTestUtils.setField(post, "id", POST_ID);
    }

    @Test
    void repostsAVisiblePostAndIncrementsItsCounter() {
        when(postAccessService.findViewablePost(USER_ID, POST_ID)).thenReturn(post);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(postRepostRepository.existsByPostIdAndUserId(POST_ID, USER_ID)).thenReturn(false);
        when(postRepostRepository.saveAndFlush(any(PostRepost.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PostRepostResponse response = postRepostService.repost(USER_ID, POST_ID);

        assertEquals(POST_ID, response.postId());
        assertEquals(USER_ID, response.userId());
        assertTrue(response.reposted());
        verify(postRepository).incrementRepostCount(POST_ID);
        verify(notificationService).notifyPostInteraction(NotificationType.POST_REPOST, user, post);
    }

    @Test
    void rejectsDuplicateRepostsBeforeWriting() {
        when(postAccessService.findViewablePost(USER_ID, POST_ID)).thenReturn(post);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(postRepostRepository.existsByPostIdAndUserId(POST_ID, USER_ID)).thenReturn(true);

        assertThrows(
                DuplicatePostRepostException.class,
                () -> postRepostService.repost(USER_ID, POST_ID));
        verify(postRepostRepository, never()).saveAndFlush(any());
        verify(postRepository, never()).incrementRepostCount(any());
        verify(notificationService, never()).notifyPostInteraction(any(), any(), any());
    }

    @Test
    void removesARepostAndDecrementsItsCounter() {
        PostRepost repost = new PostRepost(post, user);
        when(postAccessService.findActivePost(POST_ID)).thenReturn(post);
        when(postRepostRepository.findByPostIdAndUserId(POST_ID, USER_ID))
                .thenReturn(Optional.of(repost));

        postRepostService.removeRepost(USER_ID, POST_ID);

        verify(postRepostRepository).delete(repost);
        verify(postRepostRepository).flush();
        verify(postRepository).decrementRepostCount(POST_ID);
    }

    @Test
    void reportsAMissingRepost() {
        when(postAccessService.findActivePost(POST_ID)).thenReturn(post);
        when(postRepostRepository.findByPostIdAndUserId(POST_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                PostRepostNotFoundException.class,
                () -> postRepostService.removeRepost(USER_ID, POST_ID));
        verify(postRepository, never()).decrementRepostCount(any());
    }

    @Test
    void returnsTheCurrentUsersRepostStatus() {
        when(postAccessService.findViewablePost(USER_ID, POST_ID)).thenReturn(post);
        when(postRepostRepository.existsByPostIdAndUserId(POST_ID, USER_ID))
                .thenReturn(true, false);

        PostRepostStatusResponse reposted = postRepostService.getStatus(USER_ID, POST_ID);
        PostRepostStatusResponse notReposted = postRepostService.getStatus(USER_ID, POST_ID);

        assertTrue(reposted.reposted());
        assertFalse(notReposted.reposted());
    }

    @Test
    void listsRepostsWithBoundedStablePagination() {
        PostRepost repost = new PostRepost(post, user);
        when(postAccessService.findViewablePost(USER_ID, POST_ID)).thenReturn(post);
        when(postRepostRepository.findByPostId(eq(POST_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(repost), PageRequest.of(0, 100), 1));

        PageResponse<PostRepostUserResponse> response = postRepostService.getReposts(
                USER_ID,
                POST_ID,
                PageRequest.of(0, 500));

        assertEquals(USER_ID, response.content().getFirst().id());
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(postRepostRepository).findByPostId(eq(POST_ID), pageableCaptor.capture());
        assertEquals(100, pageableCaptor.getValue().getPageSize());
        assertEquals("createdAt: DESC,id: DESC", pageableCaptor.getValue().getSort().toString());
    }

    private User user(UUID id, String username) {
        User result = new User(username, "Display", username + "@example.com", "hash");
        ReflectionTestUtils.setField(result, "id", id);
        return result;
    }
}
