package com.abhiai.abhiai_backend.ai.image;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;

import com.abhiai.abhiai_backend.exception.AiProviderException;
import com.abhiai.abhiai_backend.exception.AiProviderUnavailableException;

class RoutingImageGenerationProviderTest {

    private final CloudflareImageGenerationProvider cloudflare = mock(CloudflareImageGenerationProvider.class);
    private final RoutingImageGenerationProvider router = new RoutingImageGenerationProvider(cloudflare);

    @Test
    void usesExistingCloudflareProviderAndPreservesImageResponse() {
        GeneratedImage expected = new GeneratedImage(new byte[] {1}, "image/jpeg", "flux");
        when(cloudflare.generate("Create an image of a dog")).thenReturn(expected);
        when(cloudflare.providerName()).thenReturn("cloudflare");
        when(cloudflare.modelName()).thenReturn("flux");
        when(cloudflare.configured()).thenReturn(true);

        assertSame(expected, router.generate("Create an image of a dog"));
        assertEquals("cloudflare", router.providerName());
        assertEquals("flux", router.modelName());
        assertTrue(router.configured());
        verify(cloudflare).generate("Create an image of a dog");
    }

    @Test
    void missingCloudflareCredentialsNeverSelectAnotherProvider() {
        when(cloudflare.configured()).thenReturn(false);
        when(cloudflare.generate("prompt")).thenThrow(new AiProviderUnavailableException("missing credentials"));

        assertFalse(router.configured());
        assertEquals("Image generation failed. Please try again.",
                assertThrows(AiProviderException.class, () -> router.generate("prompt")).getMessage());
        verify(cloudflare, times(1)).generate("prompt");
    }

    @Test
    void cloudflareFailureIsNeutralAndDoesNotRetryOrFallBack() {
        when(cloudflare.generate("prompt")).thenThrow(new AiProviderException("private provider diagnostic"));

        var failure = assertThrows(AiProviderException.class, () -> router.generate("prompt"));
        assertEquals("Image generation failed. Please try again.", failure.getMessage());
        assertNull(failure.getCause());
        verify(cloudflare, times(1)).generate("prompt");
        verifyNoMoreInteractions(cloudflare);
    }
}
