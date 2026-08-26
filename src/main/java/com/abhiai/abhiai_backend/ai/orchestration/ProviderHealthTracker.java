package com.abhiai.abhiai_backend.ai.orchestration;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.abhiai.abhiai_backend.exception.AiProviderException;
import com.abhiai.abhiai_backend.exception.AiProviderFailureKind;

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
        if (state == null) return true;
        if (state.terminal) return false;
        if (state.rateLimited) return cooldownElapsed(state);
        if (state.failures < FAILURE_THRESHOLD) return true;
        return cooldownElapsed(state);
    }

    public ModelStatus status(String provider, boolean configured) {
        if (!configured) return ModelStatus.UNAVAILABLE;
        State state = states.get(provider);
        if (state == null || state.failures == 0) return ModelStatus.AVAILABLE;
        if (state.terminal) return ModelStatus.UNAVAILABLE;
        if (!canAttempt(provider) && state.rateLimited) return ModelStatus.RATE_LIMITED;
        return canAttempt(provider) ? ModelStatus.DEGRADED : ModelStatus.UNAVAILABLE;
    }

    public void success(String provider) { states.remove(provider); }

    public void failure(String provider) {
        failure(provider, null);
    }

    public void failure(String provider, Throwable failure) {
        String message = failure == null || failure.getMessage() == null ? "" : failure.getMessage().toLowerCase(java.util.Locale.ROOT);
        AiProviderFailureKind kind = failure instanceof AiProviderException providerFailure
                ? providerFailure.kind()
                : AiProviderFailureKind.UNKNOWN;
        boolean rateLimited = kind == AiProviderFailureKind.RATE_LIMIT
                || message.contains("rate limit") || message.contains("quota") || message.contains("429");
        boolean terminal = switch (kind) {
            case AUTHENTICATION, AUTHORIZATION, BILLING, MODEL_UNAVAILABLE, CONFIGURATION -> true;
            default -> message.contains("rejected its api credentials")
                    || message.contains("requires available api balance")
                    || message.contains("http 402")
                    || message.contains("not configured");
        };
        states.compute(provider, (key, old) -> new State(old == null ? 1 : old.failures + 1, clock.instant(),
                rateLimited || old != null && old.rateLimited,
                terminal || old != null && old.terminal));
    }

    private boolean cooldownElapsed(State state) {
        return !clock.instant().isBefore(state.lastFailure.plus(COOLDOWN));
    }

    private record State(int failures, Instant lastFailure, boolean rateLimited, boolean terminal) { }
}
