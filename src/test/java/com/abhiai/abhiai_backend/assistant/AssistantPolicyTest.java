package com.abhiai.abhiai_backend.assistant;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import static org.junit.jupiter.api.Assertions.*;

class AssistantPolicyTest {
    @Test void disabledAndRateLimitsAreEnforcedPerUser() {
        var settings = new AssistantProperties(); settings.setRequestsPerMinute(2); settings.setSessionsPerHour(1);
        var policy = new AssistantPolicy(settings); var user = UUID.randomUUID();
        policy.request(user); policy.request(user);
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, assertThrows(AssistantException.class, () -> policy.request(user)).status());
        policy.request(UUID.randomUUID());
        policy.session(user);
        assertThrows(AssistantException.class, () -> policy.session(user));
        settings.setEnabled(false);
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(AssistantException.class, () -> policy.request(user)).status());
    }
}
