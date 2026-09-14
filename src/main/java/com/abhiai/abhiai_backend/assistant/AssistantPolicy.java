package com.abhiai.abhiai_backend.assistant;

import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Per-instance limits. Deploy one backend replica, or replace with a shared limiter. */
@Component
public class AssistantPolicy {
    private final AssistantProperties properties;
    private final Map<String, Deque<Instant>> windows = new HashMap<>();
    public AssistantPolicy(AssistantProperties properties) { this.properties = properties; }
    public void requireEnabled() {
        if (!properties.isEnabled()) throw new AssistantException(HttpStatus.NOT_FOUND, "AbhiAI Assistant is disabled.");
    }
    public synchronized void request(UUID userId) {
        requireEnabled();
        limit("request:" + userId, properties.getRequestsPerMinute(), 60);
    }
    public synchronized void session(UUID userId) {
        requireEnabled();
        limit("session:" + userId, properties.getSessionsPerHour(), 3600);
    }
    private void limit(String key, int maximum, int seconds) {
        Instant now = Instant.now();
        Deque<Instant> attempts = windows.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        while (!attempts.isEmpty() && attempts.peekFirst().isBefore(now.minusSeconds(seconds))) attempts.removeFirst();
        if (attempts.size() >= maximum) throw new AssistantException(HttpStatus.TOO_MANY_REQUESTS,
                "Assistant request limit reached. Please wait before trying again.");
        attempts.addLast(now);
    }
    @Scheduled(fixedDelay = 60_000)
    synchronized void prune() {
        Instant cutoff = Instant.now().minusSeconds(3600);
        windows.entrySet().removeIf(entry -> entry.getValue().isEmpty() || entry.getValue().peekLast().isBefore(cutoff));
    }
}
