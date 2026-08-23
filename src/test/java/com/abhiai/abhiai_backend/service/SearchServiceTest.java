package com.abhiai.abhiai_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
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

import com.abhiai.abhiai_backend.entity.Hashtag;
import com.abhiai.abhiai_backend.entity.Post;
import com.abhiai.abhiai_backend.entity.PostVisibility;
import com.abhiai.abhiai_backend.entity.User;
import com.abhiai.abhiai_backend.exception.InvalidSearchQueryException;
import com.abhiai.abhiai_backend.exception.UserNotFoundException;
import com.abhiai.abhiai_backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID POST_ID = UUID.randomUUID();

    @Mock private UserRepository userRepository;
    @Mock private SearchProvider searchProvider;

    private SearchService searchService;
    private User user;

    @BeforeEach
    void setUp() {
        searchService = new SearchService(userRepository, searchProvider);
        user = new User("search_user", "Search User", "search@example.com", "hash");
        ReflectionTestUtils.setField(user, "id", USER_ID);
    }

    @Test
    void rejectsQueriesOutsideTheAllowedLength() {
        assertThrows(InvalidSearchQueryException.class,
                () -> searchService.searchUsers("x", PageRequest.of(0, 20)));
        assertThrows(InvalidSearchQueryException.class,
                () -> searchService.searchUsers("x".repeat(101), PageRequest.of(0, 20)));
        verify(searchProvider, never()).searchUsers(any(), any());
    }

    @Test
    void searchesUsersWithTrimmedBoundedStablePagination() {
        when(searchProvider.searchUsers(eq("Search"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(user), PageRequest.of(0, 50), 1));

        var response = searchService.searchUsers("  Search  ", PageRequest.of(0, 500));

        assertEquals(USER_ID, response.content().getFirst().id());
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(searchProvider).searchUsers(eq("Search"), pageable.capture());
        assertEquals(50, pageable.getValue().getPageSize());
        assertEquals("username: ASC,id: ASC", pageable.getValue().getSort().toString());
    }

    @Test
    void rejectsPostSearchForAnUnknownAuthenticatedUser() {
        when(userRepository.existsById(USER_ID)).thenReturn(false);

        assertThrows(UserNotFoundException.class,
                () -> searchService.searchPosts(USER_ID, "AbhiAI", PageRequest.of(0, 20)));
        verify(searchProvider, never()).searchPosts(any(), any(), any(), any());
    }

    @Test
    void normalizesAdvancedPostFilters() {
        Post post = new Post(user, "Building AbhiAI search", PostVisibility.PUBLIC);
        ReflectionTestUtils.setField(post, "id", POST_ID);
        when(userRepository.existsById(USER_ID)).thenReturn(true);
        when(searchProvider.searchPosts(eq(USER_ID), eq("AbhiAI search"), any(), any()))
                .thenReturn(new PageImpl<>(List.of(post), PageRequest.of(0, 50), 1));
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-12-31T23:59:59Z");

        var response = searchService.searchPosts(
                USER_ID,
                "  AbhiAI search  ",
                new PostSearchCriteria(" @Search_User ", from, to, true, SearchSort.POPULAR),
                PageRequest.of(0, 500));

        assertEquals(POST_ID, response.content().getFirst().id());
        ArgumentCaptor<PostSearchCriteria> criteria = ArgumentCaptor.forClass(PostSearchCriteria.class);
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(searchProvider).searchPosts(
                eq(USER_ID), eq("AbhiAI search"), criteria.capture(), pageable.capture());
        assertEquals("search_user", criteria.getValue().authorUsername());
        assertEquals(SearchSort.POPULAR, criteria.getValue().sort());
        assertEquals(true, criteria.getValue().hasMedia());
        assertEquals(50, pageable.getValue().getPageSize());
    }

    @Test
    void rejectsInvalidAuthorAndReversedDateRange() {
        when(userRepository.existsById(USER_ID)).thenReturn(true);
        assertThrows(InvalidSearchQueryException.class, () -> searchService.searchPosts(
                USER_ID, "search", new PostSearchCriteria("bad user", null, null, null, null),
                PageRequest.of(0, 20)));
        assertThrows(InvalidSearchQueryException.class, () -> searchService.searchPosts(
                USER_ID, "search", new PostSearchCriteria(
                        null, Instant.parse("2026-12-01T00:00:00Z"),
                        Instant.parse("2026-01-01T00:00:00Z"), null, null),
                PageRequest.of(0, 20)));
    }

    @Test
    void searchesNormalizedHashtagsByPopularity() {
        Hashtag hashtag = new Hashtag(UUID.randomUUID(), "abhiai", "AbhiAI");
        when(searchProvider.searchHashtags(eq("abhiai"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(hashtag)));

        var response = searchService.searchHashtags(" #AbhiAI ", PageRequest.of(0, 20));

        assertEquals("abhiai", response.content().getFirst().normalizedTag());
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(searchProvider).searchHashtags(eq("abhiai"), pageable.capture());
        assertEquals("postCount: DESC,normalizedTag: ASC,id: ASC", pageable.getValue().getSort().toString());
    }
}
