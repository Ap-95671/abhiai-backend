package com.abhiai.abhiai_backend.dto.post;

import java.util.List;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreatePollRequest(
        @NotNull(message = "Poll choices are required")
        @Size(min = 2, max = 4, message = "A poll must contain between 2 and 4 choices")
        List<@NotBlank(message = "Poll choices must not be blank") @Size(max = 100, message = "Poll choices must not exceed 100 characters") String> choices,

        @Min(value = 1, message = "Poll duration must be at least 1 hour")
        @Max(value = 168, message = "Poll duration must not exceed 7 days")
        int durationHours) {
}
