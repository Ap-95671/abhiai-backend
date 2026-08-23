package com.abhiai.abhiai_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
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
import com.abhiai.abhiai_backend.dto.follow.FollowActionResponse;
import com.abhiai.abhiai_backend.dto.follow.FollowStatusResponse;
import com.abhiai.abhiai_backend.dto.follow.FollowUserResponse;
import com.abhiai.abhiai_backend.entity.Follow;
import com.abhiai.abhiai_backend.entity.User;
import com.abhiai.abhiai_backend.exception.DuplicateFollowException;
import com.abhiai.abhiai_backend.exception.FollowRelationshipNotFoundException;
import com.abhiai.abhiai_backend.exception.InvalidFollowException;
import com.abhiai.abhiai_backend.repository.FollowRepository;
import com.abhiai.abhiai_backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class FollowServiceTest {

    private static final UUID ACTING_USER_ID = UUID.randomUUID();
    private static final UUID TARGET_USER_ID = UUID.randomUUID();

    @Mock
    private FollowRepository followRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SocialNotificationService notificationService;
    @Mock private CreatorAnalyticsService analyticsService;

    private FollowService followService;
    private User actingUser;
    private User targetUser;

    @BeforeEach
    void setUp() {
        followService = new FollowService(followRepository, userRepository, notificationService, analyticsService);
        actingUser = user(ACTING_USER_ID, "acting_user", "Acting User");
        targetUser = user(TARGET_USER_ID, "target_user", "Target User");
    }

    @Test
    void rejectsFollowingYourselfBeforeAccessingTheDatabase() {
        assertThrows(
                InvalidFollowException.class,
                () -> followService.follow(ACTING_USER_ID, ACTING_USER_ID));

        verifyNoInteractions(followRepository, userRepository);
    }

    @Test
    void createsOneFollowAndUpdatesBothCounters() {
        when(userRepository.findById(ACTING_USER_ID)).thenReturn(Optional.of(actingUser));
        when(userRepository.findById(TARGET_USER_ID)).thenReturn(Optional.of(targetUser));
        when(followRepository.existsByFollowerIdAndFollowingId(ACTING_USER_ID, TARGET_USER_ID))
                .thenReturn(false);
        when(followRepository.saveAndFlush(any(Follow.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        FollowActionResponse response = followService.follow(ACTING_USER_ID, TARGET_USER_ID);

        assertEquals(TARGET_USER_ID, response.userId());
        verify(userRepository).incrementFollowingCount(ACTING_USER_ID);
        verify(userRepository).incrementFollowerCount(TARGET_USER_ID);
        verify(notificationService).notifyFollow(actingUser, targetUser);
    }

    @Test
    void rejectsADuplicateFollowWithoutChangingCounters() {
        when(userRepository.findById(ACTING_USER_ID)).thenReturn(Optional.of(actingUser));
        when(userRepository.findById(TARGET_USER_ID)).thenReturn(Optional.of(targetUser));
        when(followRepository.existsByFollowerIdAndFollowingId(ACTING_USER_ID, TARGET_USER_ID))
                .thenReturn(true);

        assertThrows(
                DuplicateFollowException.class,
                () -> followService.follow(ACTING_USER_ID, TARGET_USER_ID));

        verify(followRepository, never()).saveAndFlush(any());
        verify(userRepository, never()).incrementFollowingCount(any());
        verify(userRepository, never()).incrementFollowerCount(any());
        verifyNoInteractions(notificationService);
    }

    @Test
    void unfollowsAndDecrementsBothCounters() {
        Follow follow = new Follow(actingUser, targetUser);
        when(userRepository.findById(TARGET_USER_ID)).thenReturn(Optional.of(targetUser));
        when(followRepository.findByFollowerIdAndFollowingId(ACTING_USER_ID, TARGET_USER_ID))
                .thenReturn(Optional.of(follow));

        followService.unfollow(ACTING_USER_ID, TARGET_USER_ID);

        verify(followRepository).delete(follow);
        verify(followRepository).flush();
        verify(userRepository).decrementFollowingCount(ACTING_USER_ID);
        verify(userRepository).decrementFollowerCount(TARGET_USER_ID);
    }

    @Test
    void rejectsUnfollowingWhenTheRelationshipDoesNotExist() {
        when(userRepository.findById(TARGET_USER_ID)).thenReturn(Optional.of(targetUser));
        when(followRepository.findByFollowerIdAndFollowingId(ACTING_USER_ID, TARGET_USER_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                FollowRelationshipNotFoundException.class,
                () -> followService.unfollow(ACTING_USER_ID, TARGET_USER_ID));

        verify(userRepository, never()).decrementFollowingCount(any());
        verify(userRepository, never()).decrementFollowerCount(any());
    }

    @Test
    void reportsFollowStatusWithoutAllowingSelfFollowState() {
        when(userRepository.findById(ACTING_USER_ID)).thenReturn(Optional.of(actingUser));

        FollowStatusResponse response = followService.getFollowStatus(ACTING_USER_ID, ACTING_USER_ID);

        assertFalse(response.following());
        verifyNoInteractions(followRepository);
    }

    @Test
    void returnsFollowersWithBoundedStablePagination() {
        Instant followedAt = Instant.parse("2026-08-17T00:00:00Z");
        Follow follow = new Follow(actingUser, targetUser);
        ReflectionTestUtils.setField(follow, "createdAt", followedAt);
        when(userRepository.findById(TARGET_USER_ID)).thenReturn(Optional.of(targetUser));
        when(followRepository.findByFollowingId(eq(TARGET_USER_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(follow), PageRequest.of(0, 100), 1));

        PageResponse<FollowUserResponse> response = followService.getFollowers(
                TARGET_USER_ID,
                PageRequest.of(0, 500));

        assertEquals(1, response.totalElements());
        assertEquals("acting_user", response.content().getFirst().username());
        assertEquals(followedAt, response.content().getFirst().followedAt());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(followRepository).findByFollowingId(eq(TARGET_USER_ID), pageableCaptor.capture());
        assertEquals(100, pageableCaptor.getValue().getPageSize());
        assertEquals("createdAt: DESC,id: DESC", pageableCaptor.getValue().getSort().toString());
    }

    private User user(UUID id, String username, String displayName) {
        User user = new User(username, displayName, username + "@example.com", "hash");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
