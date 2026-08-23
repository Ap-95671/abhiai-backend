package com.abhiai.abhiai_backend.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.abhiai.abhiai_backend.entity.Follow;
import com.abhiai.abhiai_backend.entity.User;
import com.abhiai.abhiai_backend.entity.UserBlock;
import com.abhiai.abhiai_backend.exception.UnauthorizedActionException;
import com.abhiai.abhiai_backend.repository.FollowRepository;
import com.abhiai.abhiai_backend.repository.UserBlockRepository;
import com.abhiai.abhiai_backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserBlockServiceTest {
    private static final UUID ACTOR_ID = UUID.randomUUID();
    private static final UUID TARGET_ID = UUID.randomUUID();

    @Mock UserBlockRepository blockRepository;
    @Mock FollowRepository followRepository;
    @Mock UserRepository userRepository;
    @Mock CreatorAnalyticsService analyticsService;
    private UserBlockService service;
    private User actor;
    private User target;

    @BeforeEach
    void setUp() {
        service = new UserBlockService(blockRepository, followRepository, userRepository, analyticsService);
        actor = user(ACTOR_ID, "actor"); target = user(TARGET_ID, "target");
    }

    @Test
    void rejectsSelfBlock() {
        assertThrows(UnauthorizedActionException.class, () -> service.block(ACTOR_ID, ACTOR_ID));
    }

    @Test
    void blockIsIdempotentAndRemovesBothFollowDirections() {
        Follow outgoing = new Follow(actor, target);
        Follow incoming = new Follow(target, actor);
        when(userRepository.findById(ACTOR_ID)).thenReturn(Optional.of(actor));
        when(userRepository.findById(TARGET_ID)).thenReturn(Optional.of(target));
        when(blockRepository.existsByBlockerIdAndBlockedId(ACTOR_ID, TARGET_ID)).thenReturn(false, true);
        when(blockRepository.existsByBlockerIdAndBlockedId(TARGET_ID, ACTOR_ID)).thenReturn(false);
        when(followRepository.findByFollowerIdAndFollowingId(ACTOR_ID, TARGET_ID)).thenReturn(Optional.of(outgoing));
        when(followRepository.findByFollowerIdAndFollowingId(TARGET_ID, ACTOR_ID)).thenReturn(Optional.of(incoming));

        var response = service.block(ACTOR_ID, TARGET_ID);

        assertTrue(response.blockedByMe());
        verify(blockRepository).saveAndFlush(any(UserBlock.class));
        verify(followRepository).delete(outgoing);
        verify(followRepository).delete(incoming);
        verify(userRepository).decrementFollowingCount(ACTOR_ID);
        verify(userRepository).decrementFollowerCount(TARGET_ID);
        verify(userRepository).decrementFollowingCount(TARGET_ID);
        verify(userRepository).decrementFollowerCount(ACTOR_ID);
    }

    private User user(UUID id, String username) {
        User user = new User(username, username, username + "@example.com", "hash");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
