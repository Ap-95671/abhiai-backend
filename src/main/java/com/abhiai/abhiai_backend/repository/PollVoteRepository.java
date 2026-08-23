package com.abhiai.abhiai_backend.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.abhiai.abhiai_backend.entity.PollVote;

public interface PollVoteRepository extends JpaRepository<PollVote, UUID> {
    boolean existsByPollIdAndUserId(UUID pollId, UUID userId);
    Optional<PollVote> findByPollIdAndUserId(UUID pollId, UUID userId);
}
