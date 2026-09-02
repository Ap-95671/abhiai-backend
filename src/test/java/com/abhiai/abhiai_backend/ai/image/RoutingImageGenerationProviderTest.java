package com.abhiai.abhiai_backend.ai.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.abhiai.abhiai_backend.exception.AiProviderException;

class RoutingImageGenerationProviderTest {

    private final CloudflareImageGenerationProvider cloudflare = mock(CloudflareImageGenerationProvider.class);
    private final GeminiImageGenerationProvider gemini = mock(GeminiImageGenerationProvider.class);
    private final ImageGenerationRoutingProperties properties = new ImageGenerationRoutingProperties();

    @Test
    void usesCloudflareByDefault() {
        GeneratedImage expected = new GeneratedImage(new byte[] {1}, "image/jpeg", "flux");
        when(cloudflare.configured()).thenReturn(true);
        when(cloudflare.generate("prompt")).thenReturn(expected);

        GeneratedImage result = router().generate("prompt");

        assertEquals("flux", result.model());
    }

    @Test
    void fallsBackFromExplicitGeminiToConfiguredCloudflare() {
        properties.setProvider("gemini");
        when(gemini.configured()).thenReturn(true);
        when(cloudflare.configured()).thenReturn(true);
        when(gemini.generate("prompt")).thenThrow(new AiProviderException("quota"));
        when(cloudflare.generate("prompt")).thenReturn(new GeneratedImage(new byte[] {1}, "image/jpeg", "flux"));

        assertEquals("flux", router().generate("prompt").model());
    }

    @Test
    void doesNotSilentlyFallBackToGeminiFromCloudflare() {
        when(cloudflare.configured()).thenReturn(true);
        when(gemini.configured()).thenReturn(true);
        when(cloudflare.generate("prompt")).thenThrow(new AiProviderException("free quota"));

        assertThrows(AiProviderException.class, () -> router().generate("prompt"));
    }

    private RoutingImageGenerationProvider router() {
        return new RoutingImageGenerationProvider(cloudflare, gemini, properties);
    }
}
