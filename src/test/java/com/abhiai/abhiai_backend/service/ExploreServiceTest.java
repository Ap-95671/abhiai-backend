package com.abhiai.abhiai_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.abhiai.abhiai_backend.entity.Hashtag;
import com.abhiai.abhiai_backend.entity.Post;
import com.abhiai.abhiai_backend.entity.PostVisibility;
import com.abhiai.abhiai_backend.entity.User;
import com.abhiai.abhiai_backend.exception.UserNotFoundException;
import com.abhiai.abhiai_backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class ExploreServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-08-17T12:00:00Z");
    private static final Instant CUTOFF = Instant.parse("2026-07-18T12:00:00Z");

    @Mock private UserRepository userRepository;
    @Mock private ExploreRankingProvider rankingProvider;

    private ExploreService exploreService;
    private User account;
    private Post post;
    private Hashtag hashtag;

    @BeforeEach
    void setUp() {
        exploreService = new ExploreService(
                userRepository,
                rankingProvider,
                Clock.fixed(NOW, ZoneOffset.UTC));
        account = new User("explorer", "Explorer", "explorer@example.com", "hash");
        ReflectionTestUtils.setField(account, "id", USER_ID);
        post = new Post(account, "Discover #AbhiAI", PostVisibility.PUBLIC);
        ReflectionTestUtils.setField(post, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(post, "createdAt", NOW);
        ReflectionTestUtils.setField(post, "updatedAt", NOW);
        hashtag = new Hashtag(UUID.randomUUID(), "abhiai", "AbhiAI");
        ReflectionTestUtils.setField(hashtag, "createdAt", NOW);
    }

    @Test
    void buildsExploreSnapshotFromTransparentRankings() {
        when(userRepository.existsById(USER_ID)).thenReturn(true);
        when(rankingProvider.trendingPosts(CUTOFF, 8)).thenReturn(List.of(post));
        when(rankingProvider.trendingHashtags(8)).thenReturn(List.of(hashtag));
        when(rankingProvider.suggestedAccounts(USER_ID, 8)).thenReturn(List.of(account));
        when(rankingProvider.popularDiscussions(CUTOFF, 8)).thenReturn(List.of(post));
        when(rankingProvider.recommendedMedia(CUTOFF, 8)).thenReturn(List.of());

        var result = exploreService.explore(USER_ID, null);

        assertEquals(NOW, result.generatedAt());
        assertEquals(30, result.rankingWindowDays());
        assertEquals(1, result.trendingPosts().size());
        assertEquals("abhiai", result.trendingHashtags().getFirst().normalizedTag());
        assertEquals("explorer", result.suggestedAccounts().getFirst().username());
    }

    @Test
    void clampsRequestedSectionSize() {
        when(userRepository.existsById(USER_ID)).thenReturn(true);
        when(rankingProvider.trendingPosts(CUTOFF, 20)).thenReturn(List.of());
        when(rankingProvider.trendingHashtags(20)).thenReturn(List.of());
        when(rankingProvider.suggestedAccounts(USER_ID, 20)).thenReturn(List.of());
        when(rankingProvider.popularDiscussions(CUTOFF, 20)).thenReturn(List.of());
        when(rankingProvider.recommendedMedia(CUTOFF, 20)).thenReturn(List.of());

        exploreService.explore(USER_ID, 500);

        verify(rankingProvider).trendingPosts(CUTOFF, 20);
    }

    @Test
    void rejectsUnknownAuthenticatedUser() {
        when(userRepository.existsById(USER_ID)).thenReturn(false);

        assertThrows(UserNotFoundException.class, () -> exploreService.explore(USER_ID, 8));
        verify(rankingProvider, never()).trendingHashtags(8);
    }
}
