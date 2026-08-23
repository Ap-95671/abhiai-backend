package com.abhiai.abhiai_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import com.abhiai.abhiai_backend.dto.common.PageResponse;
import com.abhiai.abhiai_backend.dto.post.PostResponse;
import com.abhiai.abhiai_backend.entity.Post;
import com.abhiai.abhiai_backend.entity.PostVisibility;
import com.abhiai.abhiai_backend.entity.User;
import com.abhiai.abhiai_backend.exception.UserNotFoundException;
import com.abhiai.abhiai_backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class FeedServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID POST_ID = UUID.randomUUID();

    @Mock
    private UserRepository userRepository;

    @Mock
    private FeedStrategy feedStrategy;

    private FeedService feedService;

    @BeforeEach
    void setUp() {
        feedService = new FeedService(userRepository, feedStrategy);
    }

    @Test
    void returnsMappedPostsWithBoundedStablePagination() {
        User author = new User("author", "Author", "author@example.com", "hash");
        ReflectionTestUtils.setField(author, "id", USER_ID);
        Post post = new Post(author, "Feed post", PostVisibility.PUBLIC);
        ReflectionTestUtils.setField(post, "id", POST_ID);

        when(userRepository.existsById(USER_ID)).thenReturn(true);
        when(feedStrategy.load(eq(USER_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(post), PageRequest.of(0, 100), 1));

        PageResponse<PostResponse> response = feedService.getHomeFeed(
                USER_ID,
                PageRequest.of(0, 500, Sort.by("textContent")));

        assertEquals(1, response.totalElements());
        assertEquals(POST_ID, response.content().getFirst().id());
        assertEquals("author", response.content().getFirst().author().username());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(feedStrategy).load(eq(USER_ID), pageableCaptor.capture());
        assertEquals(100, pageableCaptor.getValue().getPageSize());
        assertEquals("createdAt: DESC,id: DESC", pageableCaptor.getValue().getSort().toString());
    }

    @Test
    void preservesRequestedPageNumber() {
        when(userRepository.existsById(USER_ID)).thenReturn(true);
        when(feedStrategy.load(eq(USER_ID), any(Pageable.class)))
                .thenAnswer(invocation -> Page.empty(invocation.getArgument(1)));

        feedService.getHomeFeed(USER_ID, PageRequest.of(3, 20));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(feedStrategy).load(eq(USER_ID), pageableCaptor.capture());
        assertEquals(3, pageableCaptor.getValue().getPageNumber());
        assertEquals(20, pageableCaptor.getValue().getPageSize());
    }

    @Test
    void rejectsADeletedOrUnknownAuthenticatedUser() {
        when(userRepository.existsById(USER_ID)).thenReturn(false);

        assertThrows(
                UserNotFoundException.class,
                () -> feedService.getHomeFeed(USER_ID, PageRequest.of(0, 20)));

        verify(feedStrategy, never()).load(any(), any());
    }
}
