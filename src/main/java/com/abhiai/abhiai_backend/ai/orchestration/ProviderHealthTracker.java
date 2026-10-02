package com.abhiai.abhiai_backend.ai.orchestration;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import com.abhiai.abhiai_backend.config.AiOrchestrationProperties;
import com.abhiai.abhiai_backend.exception.*;

/** Passive circuit breaker. Routing reads availability; execution atomically claims a half-open probe. */
@Component
public class ProviderHealthTracker {
    public enum Circuit { CLOSED, OPEN, HALF_OPEN }
    private final Map<String, State> states = new HashMap<>();
    private final Clock clock;
    private final AiOrchestrationProperties properties;
    public ProviderHealthTracker() { this(Clock.systemUTC(), new AiOrchestrationProperties()); }
    ProviderHealthTracker(Clock clock) { this(clock, new AiOrchestrationProperties()); }
    @Autowired public ProviderHealthTracker(AiOrchestrationProperties properties) { this(Clock.systemUTC(), properties); }
    ProviderHealthTracker(Clock clock, AiOrchestrationProperties properties) { this.clock = clock; this.properties = properties; }

    public synchronized boolean canAttempt(String provider) {
        State state = states.get(provider);
        return state == null || state.openUntil == null || (!clock.instant().isBefore(state.openUntil)
                && (state.probeUntil == null || !clock.instant().isBefore(state.probeUntil)));
    }
    public synchronized boolean tryAcquire(String provider) {
        if (!canAttempt(provider)) return false;
        State state = states.get(provider);
        if (state != null && state.openUntil != null) state.probeUntil = clock.instant().plusSeconds(120);
        return true;
    }
    public synchronized void abandon(String provider) {
        State state = states.get(provider);
        if (state != null) state.probeUntil = null;
    }
    public synchronized Circuit circuit(String provider) {
        State state = states.get(provider);
        if (state == null || state.openUntil == null) return Circuit.CLOSED;
        return clock.instant().isBefore(state.openUntil) ? Circuit.OPEN : Circuit.HALF_OPEN;
    }
    public synchronized ModelStatus status(String provider, boolean configured) {
        if (!configured) return ModelStatus.UNAVAILABLE;
        State state = states.get(provider);
        if (state == null) return ModelStatus.AVAILABLE;
        if (circuit(provider) == Circuit.OPEN) return state.rateLimited ? ModelStatus.RATE_LIMITED : ModelStatus.UNAVAILABLE;
        return ModelStatus.DEGRADED;
    }
    public synchronized void success(String provider) {
        // An older in-flight success must not immediately close a newly opened circuit.
        if (circuit(provider) != Circuit.OPEN) states.remove(provider);
    }
    public void failure(String provider) { failure(provider, null); }
    public synchronized void failure(String provider, Throwable failure) {
        var kind = AiProviderFailureKind.classify(failure);
        if (kind == AiProviderFailureKind.INVALID_REQUEST || kind == AiProviderFailureKind.CONTENT_RESTRICTION) {
            abandon(provider); return;
        }
        String message = failure == null || failure.getMessage() == null ? "" : failure.getMessage().toLowerCase(java.util.Locale.ROOT);
        boolean rateLimited = kind == AiProviderFailureKind.RATE_LIMIT || message.contains("429") || message.contains("quota");
        boolean terminal = switch (kind) {
            case AUTHENTICATION, AUTHORIZATION, BILLING, MODEL_UNAVAILABLE, CONFIGURATION -> true;
            default -> false;
        };
        State state = states.computeIfAbsent(provider, ignored -> new State());
        state.failures++;
        state.rateLimited = rateLimited;
        if (terminal || rateLimited || state.openUntil != null || state.failures >= properties.getCircuitFailureThreshold())
            state.openUntil = clock.instant().plus(terminal ? properties.getAuthenticationCooldown() : properties.getCircuitCooldown());
        state.probeUntil = null;
    }
    private static class State { int failures; boolean rateLimited; Instant openUntil; Instant probeUntil; }
}
