package com.abhiai.abhiai_backend.dto.post;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.abhiai.abhiai_backend.entity.PostPoll;

public record PollResponse(
        UUID id,
        List<PollChoiceResponse> choices,
        long totalVotes,
        Instant expiresAt,
        boolean expired,
        UUID selectedChoiceId) {
    public static PollResponse from(PostPoll poll, UUID selectedChoiceId) {
        return new PollResponse(
                poll.getId(), poll.getChoices().stream().map(PollChoiceResponse::from).toList(),
                poll.getTotalVotes(), poll.getExpiresAt(), poll.isExpired(), selectedChoiceId);
    }
}
