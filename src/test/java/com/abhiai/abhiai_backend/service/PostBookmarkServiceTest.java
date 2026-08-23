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

import com.abhiai.abhiai_backend.dto.bookmark.BookmarkedPostResponse;
import com.abhiai.abhiai_backend.dto.bookmark.PostBookmarkResponse;
import com.abhiai.abhiai_backend.dto.bookmark.PostBookmarkStatusResponse;
import com.abhiai.abhiai_backend.dto.common.PageResponse;
import com.abhiai.abhiai_backend.entity.Post;
import com.abhiai.abhiai_backend.entity.PostBookmark;
import com.abhiai.abhiai_backend.entity.PostVisibility;
import com.abhiai.abhiai_backend.entity.User;
import com.abhiai.abhiai_backend.exception.DuplicatePostBookmarkException;
import com.abhiai.abhiai_backend.exception.PostBookmarkNotFoundException;
import com.abhiai.abhiai_backend.exception.UserNotFoundException;
import com.abhiai.abhiai_backend.repository.PostBookmarkRepository;
import com.abhiai.abhiai_backend.repository.PostRepository;
import com.abhiai.abhiai_backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class PostBookmarkServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID POST_ID = UUID.randomUUID();

    @Mock
    private PostBookmarkRepository postBookmarkRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PostAccessService postAccessService;

    private PostBookmarkService postBookmarkService;
    private User user;
    private Post post;

    @BeforeEach
    void setUp() {
        postBookmarkService = new PostBookmarkService(
                postBookmarkRepository,
                postRepository,
                userRepository,
                postAccessService);
        user = user(USER_ID, "bookmarker");
        post = new Post(user, "Post", PostVisibility.PUBLIC);
        ReflectionTestUtils.setField(post, "id", POST_ID);
    }

    @Test
    void bookmarksAVisiblePostAndIncrementsItsCounter() {
        when(postAccessService.findViewablePost(USER_ID, POST_ID)).thenReturn(post);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(postBookmarkRepository.existsByPostIdAndUserId(POST_ID, USER_ID)).thenReturn(false);
        when(postBookmarkRepository.saveAndFlush(any(PostBookmark.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PostBookmarkResponse response = postBookmarkService.bookmark(USER_ID, POST_ID);

        assertEquals(POST_ID, response.postId());
        assertTrue(response.bookmarked());
        verify(postRepository).incrementBookmarkCount(POST_ID);
    }

    @Test
    void rejectsDuplicateBookmarksBeforeWriting() {
        when(postAccessService.findViewablePost(USER_ID, POST_ID)).thenReturn(post);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(postBookmarkRepository.existsByPostIdAndUserId(POST_ID, USER_ID)).thenReturn(true);

        assertThrows(
                DuplicatePostBookmarkException.class,
                () -> postBookmarkService.bookmark(USER_ID, POST_ID));
        verify(postBookmarkRepository, never()).saveAndFlush(any());
        verify(postRepository, never()).incrementBookmarkCount(any());
    }

    @Test
    void removesABookmarkAndDecrementsItsCounter() {
        PostBookmark bookmark = new PostBookmark(post, user);
        when(postAccessService.findActivePost(POST_ID)).thenReturn(post);
        when(postBookmarkRepository.findByPostIdAndUserId(POST_ID, USER_ID))
                .thenReturn(Optional.of(bookmark));

        postBookmarkService.removeBookmark(USER_ID, POST_ID);

        verify(postBookmarkRepository).delete(bookmark);
        verify(postBookmarkRepository).flush();
        verify(postRepository).decrementBookmarkCount(POST_ID);
    }

    @Test
    void reportsAMissingBookmark() {
        when(postAccessService.findActivePost(POST_ID)).thenReturn(post);
        when(postBookmarkRepository.findByPostIdAndUserId(POST_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                PostBookmarkNotFoundException.class,
                () -> postBookmarkService.removeBookmark(USER_ID, POST_ID));
        verify(postRepository, never()).decrementBookmarkCount(any());
    }

    @Test
    void returnsTheCurrentUsersBookmarkStatus() {
        when(postAccessService.findViewablePost(USER_ID, POST_ID)).thenReturn(post);
        when(postBookmarkRepository.existsByPostIdAndUserId(POST_ID, USER_ID))
                .thenReturn(true, false);

        PostBookmarkStatusResponse bookmarked = postBookmarkService.getStatus(USER_ID, POST_ID);
        PostBookmarkStatusResponse notBookmarked = postBookmarkService.getStatus(USER_ID, POST_ID);

        assertTrue(bookmarked.bookmarked());
        assertFalse(notBookmarked.bookmarked());
    }

    @Test
    void listsOnlyTheCurrentUsersAccessibleBookmarksWithStablePagination() {
        PostBookmark bookmark = new PostBookmark(post, user);
        when(userRepository.existsById(USER_ID)).thenReturn(true);
        when(postBookmarkRepository.findAccessibleByUserId(
                eq(USER_ID),
                eq(PostVisibility.PUBLIC),
                eq(PostVisibility.FOLLOWERS),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(bookmark), PageRequest.of(0, 100), 1));

        PageResponse<BookmarkedPostResponse> response = postBookmarkService.getBookmarks(
                USER_ID,
                PageRequest.of(0, 500));

        assertEquals(POST_ID, response.content().getFirst().post().id());
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(postBookmarkRepository).findAccessibleByUserId(
                eq(USER_ID),
                eq(PostVisibility.PUBLIC),
                eq(PostVisibility.FOLLOWERS),
                pageableCaptor.capture());
        assertEquals(100, pageableCaptor.getValue().getPageSize());
        assertEquals("createdAt: DESC,id: DESC", pageableCaptor.getValue().getSort().toString());
    }

    @Test
    void rejectsADeletedOrUnknownAuthenticatedUserBeforeListingBookmarks() {
        when(userRepository.existsById(USER_ID)).thenReturn(false);

        assertThrows(
                UserNotFoundException.class,
                () -> postBookmarkService.getBookmarks(USER_ID, PageRequest.of(0, 20)));
        verify(postBookmarkRepository, never()).findAccessibleByUserId(
                any(), any(), any(), any());
    }

    private User user(UUID id, String username) {
        User result = new User(username, "Display", username + "@example.com", "hash");
        ReflectionTestUtils.setField(result, "id", id);
        return result;
    }
}
