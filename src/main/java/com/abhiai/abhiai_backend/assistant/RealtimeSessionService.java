package com.abhiai.abhiai_backend.assistant;

import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;

@Service
public class RealtimeSessionService {
    public record SessionResponse(UUID id, String token, String model, Instant expiresAt, int idleSeconds) {
        @Override public String toString() { return "GeminiLiveSession[id=" + id + ", token=REDACTED]"; }
    }
    private record Lease(UUID userId, Instant expiresAt) {}
    private final Map<UUID, Lease> leases = new HashMap<>();
    @org.springframework.beans.factory.annotation.Autowired(required=false) private AssistantPreferencesService preferences;
    private final RealtimeGateway gateway;
    private final AssistantProperties settings;
    private final AssistantPolicy policy;
    public RealtimeSessionService(RealtimeGateway gateway, AssistantProperties settings, AssistantPolicy policy) {
        this.gateway = gateway; this.settings = settings; this.policy = policy;
    }
    public SessionResponse create(UUID userId, UUID id) {
        policy.session(userId);
        Instant expires = Instant.now().plusSeconds(settings.getMaxSessionSeconds());
        Lease lease = new Lease(userId, expires);
        synchronized (leases) {
            reap();
            if (leases.containsKey(id) || leases.values().stream().filter(l -> l.userId().equals(userId)).count()
                    >= settings.getConcurrentSessions())
                throw new AssistantException(HttpStatus.CONFLICT, "A voice session is already open. Close it before starting another.");
            leases.put(id, lease);
        }
        try {
            String token = preferences==null?gateway.create(expires):gateway.create(expires,preferences.get(userId).mode());
            synchronized (leases) {
                if (leases.get(id) != lease) throw new AssistantException(HttpStatus.CONFLICT, "Voice connection was cancelled. Please reconnect.");
            }
            return new SessionResponse(id, token, settings.getModel(), expires, settings.getIdleSeconds());
        } catch (RuntimeException exception) {
            synchronized (leases) { leases.remove(id, lease); }
            throw exception;
        }
    }
    public void close(UUID userId, UUID id) {
        synchronized (leases) {
            Lease lease = leases.get(id);
            if (lease == null) return;
            if (!lease.userId().equals(userId)) throw new AssistantException(HttpStatus.NOT_FOUND, "Voice session not found.");
            // No Gemini per-call hangup endpoint: the browser closes its socket.
            // This is an application lease, not authoritative provider concurrency enforcement.
            leases.remove(id);
        }
    }
    @Scheduled(fixedDelay = 5000)
    void reap() {
        synchronized (leases) { leases.values().removeIf(l -> !l.expiresAt().isAfter(Instant.now())); }
    }
}
