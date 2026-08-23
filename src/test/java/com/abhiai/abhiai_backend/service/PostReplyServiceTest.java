package com.abhiai.abhiai_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

import com.abhiai.abhiai_backend.config.PostProperties;
import com.abhiai.abhiai_backend.dto.common.PageResponse;
import com.abhiai.abhiai_backend.dto.reply.CreateReplyRequest;
import com.abhiai.abhiai_backend.dto.reply.PostReplyResponse;
import com.abhiai.abhiai_backend.entity.Post;
import com.abhiai.abhiai_backend.entity.PostReply;
import com.abhiai.abhiai_backend.entity.NotificationType;
import com.abhiai.abhiai_backend.entity.PostVisibility;
import com.abhiai.abhiai_backend.entity.User;
import com.abhiai.abhiai_backend.exception.InvalidReplyException;
import com.abhiai.abhiai_backend.exception.PostReplyNotFoundException;
import com.abhiai.abhiai_backend.exception.UnauthorizedActionException;
import com.abhiai.abhiai_backend.repository.PostReplyRepository;
import com.abhiai.abhiai_backend.repository.PostRepository;
import com.abhiai.abhiai_backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class PostReplyServiceTest {

    private static final UUID AUTHOR_ID = UUID.randomUUID();
    private static final UUID OTHER_USER_ID = UUID.randomUUID();
    private static final UUID POST_ID = UUID.randomUUID();
    private static final UUID REPLY_ID = UUID.randomUUID();

    @Mock
    private PostReplyRepository postReplyRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PostAccessService postAccessService;

    @Mock
    private SocialNotificationService notificationService;
    @Mock private CreatorAnalyticsService analyticsService;

    private PostReplyService postReplyService;
    private User author;
    private Post post;

    @BeforeEach
    void setUp() {
        PostProperties properties = new PostProperties();
        properties.setMaxTextLength(20);
        postReplyService = new PostReplyService(
                postReplyRepository,
                postRepository,
                userRepository,
                postAccessService,
                properties,
                notificationService,
                analyticsService);
        author = user(AUTHOR_ID, "reply_author");
        post = new Post(author, "Post", PostVisibility.PUBLIC);
        ReflectionTestUtils.setField(post, "id", POST_ID);
    }

    @Test
    void createsAReplyAndIncrementsThePostCounter() {
        when(postAccessService.findViewablePost(AUTHOR_ID, POST_ID)).thenReturn(post);
        when(userRepository.findById(AUTHOR_ID)).thenReturn(Optional.of(author));
        when(postReplyRepository.saveAndFlush(any(PostReply.class)))
                .thenAnswer(invocation -> {
                    PostReply reply = invocation.getArgument(0);
                    ReflectionTestUtils.setField(reply, "id", REPLY_ID);
                    return reply;
                });

        PostReplyResponse response = postReplyService.createReply(
                AUTHOR_ID,
                POST_ID,
                new CreateReplyRequest("  Great post!  "));

        assertEquals(REPLY_ID, response.id());
        assertEquals("Great post!", response.textContent());
        verify(postRepository).incrementReplyCount(POST_ID);
        verify(notificationService).notifyPostInteraction(NotificationType.POST_REPLY, author, post);
    }

    @Test
    void rejectsRepliesLongerThanTheConfiguredLimitBeforeDatabaseAccess() {
        assertThrows(
                InvalidReplyException.class,
                () -> postReplyService.createReply(
                        AUTHOR_ID,
                        POST_ID,
                        new CreateReplyRequest("This reply is longer than twenty characters")));

        verify(postAccessService, never()).findViewablePost(any(), any());
        verify(postReplyRepository, never()).saveAndFlush(any());
    }

    @Test
    void listsOnlyActiveRepliesWithBoundedStablePagination() {
        PostReply reply = reply(author);
        when(postAccessService.findViewablePost(AUTHOR_ID, POST_ID)).thenReturn(post);
        when(postReplyRepository.findByPostIdAndDeletedAtIsNull(eq(POST_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(reply), PageRequest.of(0, 100), 1));

        PageResponse<PostReplyResponse> response = postReplyService.getReplies(
                AUTHOR_ID,
                POST_ID,
                PageRequest.of(0, 500));

        assertEquals(REPLY_ID, response.content().getFirst().id());
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(postReplyRepository)
                .findByPostIdAndDeletedAtIsNull(eq(POST_ID), pageableCaptor.capture());
        assertEquals(100, pageableCaptor.getValue().getPageSize());
        assertEquals("createdAt: DESC,id: DESC", pageableCaptor.getValue().getSort().toString());
    }

    @Test
    void softDeletesAnOwnedReplyAndDecrementsTheCounter() {
        PostReply reply = reply(author);
        when(postAccessService.findActivePost(POST_ID)).thenReturn(post);
        when(postReplyRepository.findByIdAndPostIdAndDeletedAtIsNull(REPLY_ID, POST_ID))
                .thenReturn(Optional.of(reply));
        when(postReplyRepository.saveAndFlush(reply)).thenReturn(reply);

        postReplyService.deleteReply(AUTHOR_ID, POST_ID, REPLY_ID);

        assertNotNull(reply.getDeletedAt());
        verify(postReplyRepository).saveAndFlush(reply);
        verify(postRepository).decrementReplyCount(POST_ID);
    }

    @Test
    void preventsAnotherUserFromDeletingAReply() {
        PostReply reply = reply(author);
        when(postAccessService.findActivePost(POST_ID)).thenReturn(post);
        when(postReplyRepository.findByIdAndPostIdAndDeletedAtIsNull(REPLY_ID, POST_ID))
                .thenReturn(Optional.of(reply));

        assertThrows(
                UnauthorizedActionException.class,
                () -> postReplyService.deleteReply(OTHER_USER_ID, POST_ID, REPLY_ID));

        verify(postReplyRepository, never()).saveAndFlush(any());
        verify(postRepository, never()).decrementReplyCount(any());
    }

    @Test
    void reportsADeletedOrUnknownReplyAsNotFound() {
        when(postAccessService.findActivePost(POST_ID)).thenReturn(post);
        when(postReplyRepository.findByIdAndPostIdAndDeletedAtIsNull(REPLY_ID, POST_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                PostReplyNotFoundException.class,
                () -> postReplyService.deleteReply(AUTHOR_ID, POST_ID, REPLY_ID));
    }

    private PostReply reply(User replyAuthor) {
        PostReply reply = new PostReply(post, replyAuthor, "Reply");
        ReflectionTestUtils.setField(reply, "id", REPLY_ID);
        return reply;
    }

    private User user(UUID id, String username) {
        User result = new User(username, "Display", username + "@example.com", "hash");
        ReflectionTestUtils.setField(result, "id", id);
        return result;
    }
}
