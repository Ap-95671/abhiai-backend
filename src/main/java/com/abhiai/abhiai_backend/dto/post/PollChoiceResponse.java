package com.abhiai.abhiai_backend.dto.post;

import java.util.UUID;
import com.abhiai.abhiai_backend.entity.PollChoice;

public record PollChoiceResponse(UUID id, String text, short position, long voteCount) {
    public static PollChoiceResponse from(PollChoice choice) {
        return new PollChoiceResponse(choice.getId(), choice.getText(), choice.getPosition(), choice.getVoteCount());
    }
}
