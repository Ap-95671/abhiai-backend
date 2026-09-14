package com.abhiai.abhiai_backend.assistant;

import java.util.UUID;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RealtimeSessionServiceTest {
    @Test void failureReleasesReservationAndConcurrentSessionsAreBlocked() {
        var settings = new AssistantProperties(); var gateway = mock(RealtimeGateway.class);
        var service = new RealtimeSessionService(gateway, settings, new AssistantPolicy(settings));
        UUID user = UUID.randomUUID(), id = UUID.randomUUID();
        when(gateway.create(any())).thenThrow(new IllegalStateException("private error")).thenReturn("auth_tokens/test");
        assertThrows(IllegalStateException.class, () -> service.create(user, id));
        var response = service.create(user, id);
        assertEquals(id, response.id()); assertFalse(response.toString().contains("auth_tokens/test"));
        assertThrows(AssistantException.class, () -> service.create(user, UUID.randomUUID()));
        assertThrows(AssistantException.class, () -> service.close(UUID.randomUUID(), id));
        service.close(user, id); service.close(user, id);
        assertNotNull(service.create(user, UUID.randomUUID()));
    }
    @Test void closingDuringProvisioningNeverDeliversLateToken() throws Exception {
        var settings = new AssistantProperties(); var gateway = mock(RealtimeGateway.class);
        var service = new RealtimeSessionService(gateway, settings, new AssistantPolicy(settings));
        var entered = new CountDownLatch(1); var release = new CountDownLatch(1);
        when(gateway.create(any())).thenAnswer(invocation -> {
            entered.countDown(); assertTrue(release.await(3, TimeUnit.SECONDS)); return "auth_tokens/late";
        });
        UUID user = UUID.randomUUID(), id = UUID.randomUUID();
        var pending = CompletableFuture.runAsync(() -> service.create(user, id));
        assertTrue(entered.await(3, TimeUnit.SECONDS)); service.close(user, id); release.countDown();
        var failure = assertThrows(ExecutionException.class, () -> pending.get(3, TimeUnit.SECONDS));
        assertInstanceOf(AssistantException.class, failure.getCause());
    }
    @Test void expiredReservationsAreReaped() {
        var settings = new AssistantProperties(); settings.setMaxSessionSeconds(-1);
        var gateway = mock(RealtimeGateway.class); when(gateway.create(any())).thenReturn("auth_tokens/test");
        var service = new RealtimeSessionService(gateway, settings, new AssistantPolicy(settings));
        UUID user = UUID.randomUUID(); service.create(user, UUID.randomUUID()); service.reap();
        assertNotNull(service.create(user, UUID.randomUUID()));
    }
}
