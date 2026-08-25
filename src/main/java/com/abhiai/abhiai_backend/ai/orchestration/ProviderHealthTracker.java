package com.abhiai.abhiai_backend.ai.orchestration;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

@Component
public class ProviderHealthTracker {
    private static final int FAILURE_THRESHOLD = 3;
    private static final Duration COOLDOWN = Duration.ofSeconds(45);
    private final Map<String, State> states = new ConcurrentHashMap<>();
    private final Clock clock;

    public ProviderHealthTracker() { this(Clock.systemUTC()); }
    ProviderHealthTracker(Clock clock) { this.clock = clock; }

    public boolean canAttempt(String provider) {
        State state = states.get(provider);
        if (state == null || state.failures < FAILURE_THRESHOLD) return true;
        return state.lastFailure.plus(COOLDOWN).isBefore(clock.instant());
    }

    public ModelStatus status(String provider, boolean configured) {
        if (!configured) return ModelStatus.UNAVAILABLE;
        State state = states.get(provider);
        if (state == null || state.failures == 0) return ModelStatus.AVAILABLE;
        if (!canAttempt(provider) && state.rateLimited) return ModelStatus.RATE_LIMITED;
        return canAttempt(provider) ? ModelStatus.DEGRADED : ModelStatus.UNAVAILABLE;
    }

    public void success(String provider) { states.remove(provider); }

    public void failure(String provider) {
        failure(provider, null);
    }

    public void failure(String provider, Throwable failure) {
        String message = failure == null || failure.getMessage() == null ? "" : failure.getMessage().toLowerCase(java.util.Locale.ROOT);
        boolean rateLimited = message.contains("rate limit") || message.contains("quota") || message.contains("429");
        states.compute(provider, (key, old) -> new State(old == null ? 1 : old.failures + 1, clock.instant(),
                rateLimited || old != null && old.rateLimited));
    }

    private record State(int failures, Instant lastFailure, boolean rateLimited) { }
}
