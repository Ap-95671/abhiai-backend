package com.abhiai.abhiai_backend.dto.post;

import java.util.UUID;
import jakarta.validation.constraints.NotNull;

public record CastPollVoteRequest(@NotNull(message = "Poll choice is required") UUID choiceId) {}
