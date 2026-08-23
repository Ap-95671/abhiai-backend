package com.abhiai.abhiai_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.abhiai.abhiai_backend.entity.Post;
import com.abhiai.abhiai_backend.entity.PostVisibility;
import com.abhiai.abhiai_backend.repository.PostRepository;

@ExtendWith(MockitoExtension.class)
class ChronologicalFeedStrategyTest {

    @Mock
    private PostRepository postRepository;

    @Test
    void loadsOnlyVisibilityLevelsAvailableFromFollowedAccounts() {
        UUID userId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);
        when(postRepository.findChronologicalHomeFeed(eq(userId), org.mockito.ArgumentMatchers.anySet(), eq(pageable)))
                .thenReturn(Page.empty(pageable));
        ChronologicalFeedStrategy strategy = new ChronologicalFeedStrategy(postRepository);

        strategy.load(userId, pageable);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Set<PostVisibility>> visibilityCaptor = ArgumentCaptor.forClass(Set.class);
        verify(postRepository).findChronologicalHomeFeed(
                eq(userId),
                visibilityCaptor.capture(),
                eq(pageable));
        assertEquals(2, visibilityCaptor.getValue().size());
        assertTrue(visibilityCaptor.getValue().contains(PostVisibility.PUBLIC));
        assertTrue(visibilityCaptor.getValue().contains(PostVisibility.FOLLOWERS));
        assertFalse(visibilityCaptor.getValue().contains(PostVisibility.PRIVATE));
    }
}
