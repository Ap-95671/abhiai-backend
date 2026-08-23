package com.abhiai.abhiai_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.abhiai.abhiai_backend.entity.PollChoice;
import com.abhiai.abhiai_backend.entity.PollVote;
import com.abhiai.abhiai_backend.entity.Post;
import com.abhiai.abhiai_backend.entity.PostPoll;
import com.abhiai.abhiai_backend.entity.PostVisibility;
import com.abhiai.abhiai_backend.entity.User;
import com.abhiai.abhiai_backend.exception.DuplicatePollVoteException;
import com.abhiai.abhiai_backend.exception.InvalidPollException;
import com.abhiai.abhiai_backend.repository.PollVoteRepository;
import com.abhiai.abhiai_backend.repository.PostPollRepository;
import com.abhiai.abhiai_backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class PollServiceTest {
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID POST_ID = UUID.randomUUID();
    private static final UUID POLL_ID = UUID.randomUUID();
    private static final UUID CHOICE_ID = UUID.randomUUID();

    @Mock PostPollRepository pollRepository;
    @Mock PollVoteRepository voteRepository;
    @Mock UserRepository userRepository;
    @Mock PostAccessService postAccessService;

    private PollService service;
    private User user;
    private Post post;
    private PostPoll poll;
    private PollChoice choice;

    @BeforeEach
    void setUp() {
        service = new PollService(pollRepository, voteRepository, userRepository, postAccessService);
        user = new User("voter", "Voter", "voter@example.com", "hash");
        ReflectionTestUtils.setField(user, "id", USER_ID);
        post = new Post(user, "Best language?", PostVisibility.PUBLIC);
        ReflectionTestUtils.setField(post, "id", POST_ID);
        poll = new PostPoll(post, Instant.now().plusSeconds(3600), List.of("Java", "Python"));
        ReflectionTestUtils.setField(poll, "id", POLL_ID);
        choice = poll.getChoices().getFirst();
        ReflectionTestUtils.setField(choice, "id", CHOICE_ID);
    }

    @Test
    void castsOneVoteAndUpdatesAtomicCounters() {
        when(pollRepository.findLockedByPostId(POST_ID)).thenReturn(Optional.of(poll));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(pollRepository.saveAndFlush(poll)).thenReturn(poll);

        var response = service.vote(USER_ID, POST_ID, CHOICE_ID);

        assertEquals(1, response.totalVotes());
        assertEquals(1, response.choices().getFirst().voteCount());
        assertEquals(CHOICE_ID, response.selectedChoiceId());
        verify(voteRepository).saveAndFlush(any(PollVote.class));
    }

    @Test
    void rejectsASecondVoteFromTheSameAccount() {
        when(pollRepository.findLockedByPostId(POST_ID)).thenReturn(Optional.of(poll));
        when(voteRepository.existsByPollIdAndUserId(POLL_ID, USER_ID)).thenReturn(true);

        assertThrows(DuplicatePollVoteException.class,
                () -> service.vote(USER_ID, POST_ID, CHOICE_ID));

        verify(voteRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsVotesAfterExpiration() {
        ReflectionTestUtils.setField(poll, "expiresAt", Instant.now().minusSeconds(1));
        when(pollRepository.findLockedByPostId(POST_ID)).thenReturn(Optional.of(poll));

        assertThrows(InvalidPollException.class,
                () -> service.vote(USER_ID, POST_ID, CHOICE_ID));
    }

    @Test
    void rejectsAChoiceFromAnotherPoll() {
        when(pollRepository.findLockedByPostId(POST_ID)).thenReturn(Optional.of(poll));

        assertThrows(InvalidPollException.class,
                () -> service.vote(USER_ID, POST_ID, UUID.randomUUID()));
    }
}
