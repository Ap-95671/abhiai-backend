package com.abhiai.abhiai_backend.ai.image;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import com.abhiai.abhiai_backend.exception.AiProviderException;
import com.abhiai.abhiai_backend.exception.AiProviderUnavailableException;

@Primary
@Service
public class RoutingImageGenerationProvider implements ImageGenerationProvider {

    private final CloudflareImageGenerationProvider cloudflare;
    private final GeminiImageGenerationProvider gemini;
    private final ImageGenerationRoutingProperties properties;

    public RoutingImageGenerationProvider(
            CloudflareImageGenerationProvider cloudflare,
            GeminiImageGenerationProvider gemini,
            ImageGenerationRoutingProperties properties) {
        this.cloudflare = cloudflare;
        this.gemini = gemini;
        this.properties = properties;
    }

    @Override
    public String providerName() { return selectedProvider().providerName(); }

    @Override
    public String modelName() { return selectedProvider().modelName(); }

    @Override
    public boolean configured() { return cloudflare.configured() || gemini.configured(); }

    @Override
    public GeneratedImage generate(String prompt) {
        ImageGenerationProvider primary = selectedProvider();
        try {
            return primary.generate(prompt);
        } catch (AiProviderException primaryFailure) {
            ImageGenerationProvider fallback = fallbackFor(primary);
            if (fallback == null || !fallback.configured()) throw primaryFailure;
            return fallback.generate(prompt);
        }
    }

    private ImageGenerationProvider selectedProvider() {
        if ("gemini".equalsIgnoreCase(properties.getProvider())) {
            if (gemini.configured()) return gemini;
            if (cloudflare.configured()) return cloudflare;
        } else {
            if (cloudflare.configured()) return cloudflare;
            if (gemini.configured()) return gemini;
        }
        throw new AiProviderUnavailableException("Image generation is not configured.");
    }

    private ImageGenerationProvider fallbackFor(ImageGenerationProvider primary) {
        if (primary == gemini && cloudflare.configured()) return cloudflare;
        if (primary == cloudflare && properties.isGeminiFallbackEnabled() && gemini.configured()) return gemini;
        return null;
    }
}
