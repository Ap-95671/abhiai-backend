package com.abhiai.abhiai_backend.service;

import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.abhiai.abhiai_backend.dto.post.PollResponse;
import com.abhiai.abhiai_backend.entity.PollChoice;
import com.abhiai.abhiai_backend.entity.PollVote;
import com.abhiai.abhiai_backend.entity.PostPoll;
import com.abhiai.abhiai_backend.entity.User;
import com.abhiai.abhiai_backend.exception.DuplicatePollVoteException;
import com.abhiai.abhiai_backend.exception.InvalidPollException;
import com.abhiai.abhiai_backend.exception.PollNotFoundException;
import com.abhiai.abhiai_backend.exception.UserNotFoundException;
import com.abhiai.abhiai_backend.repository.PollVoteRepository;
import com.abhiai.abhiai_backend.repository.PostPollRepository;
import com.abhiai.abhiai_backend.repository.UserRepository;

@Service
public class PollService {
    private final PostPollRepository pollRepository;
    private final PollVoteRepository voteRepository;
    private final UserRepository userRepository;
    private final PostAccessService postAccessService;

    public PollService(PostPollRepository pollRepository, PollVoteRepository voteRepository,
            UserRepository userRepository, PostAccessService postAccessService) {
        this.pollRepository = pollRepository; this.voteRepository = voteRepository;
        this.userRepository = userRepository; this.postAccessService = postAccessService;
    }

    @Transactional(readOnly = true)
    public PollResponse getPoll(UUID userId, UUID postId) {
        postAccessService.findViewablePost(userId, postId);
        PostPoll poll = pollRepository.findByPostId(postId).orElseThrow(PollNotFoundException::new);
        UUID selected = voteRepository.findByPollIdAndUserId(poll.getId(), userId)
                .map(vote -> vote.getChoice().getId()).orElse(null);
        return PollResponse.from(poll, selected);
    }

    @Transactional
    public PollResponse vote(UUID userId, UUID postId, UUID choiceId) {
        postAccessService.findViewablePost(userId, postId);
        PostPoll poll = pollRepository.findLockedByPostId(postId).orElseThrow(PollNotFoundException::new);
        if (poll.isExpired()) throw new InvalidPollException("This poll has expired");
        if (voteRepository.existsByPollIdAndUserId(poll.getId(), userId)) throw new DuplicatePollVoteException();
        PollChoice choice = poll.getChoices().stream().filter(item -> choiceId.equals(item.getId()))
                .findFirst().orElseThrow(() -> new InvalidPollException("Choice does not belong to this poll"));
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        try {
            voteRepository.saveAndFlush(new PollVote(poll, choice, user));
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicatePollVoteException();
        }
        poll.recordVote(choice);
        pollRepository.saveAndFlush(poll);
        return PollResponse.from(poll, choice.getId());
    }
}
