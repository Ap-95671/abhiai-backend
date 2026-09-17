package com.abhiai.abhiai_backend.ai.image;

import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import com.abhiai.abhiai_backend.exception.AiProviderException;

/** Image requests use Cloudflare exclusively, independently of text/chat provider settings. */
@Primary
@Service
public class RoutingImageGenerationProvider implements ImageGenerationProvider {

    private final CloudflareImageGenerationProvider cloudflare;

    public RoutingImageGenerationProvider(CloudflareImageGenerationProvider cloudflare) {
        this.cloudflare = cloudflare;
    }

    @Override
    public String providerName() { return cloudflare.providerName(); }

    @Override
    public String modelName() { return cloudflare.modelName(); }

    @Override
    public boolean configured() { return cloudflare.configured(); }

    @Override
    public GeneratedImage generate(String prompt) {
        try {
            GeneratedImage image = cloudflare.generate(prompt);
            LoggerFactory.getLogger(getClass()).info("image_generation provider=cloudflare outcome=success");
            return image;
        } catch (RuntimeException failure) {
            // Do not log provider bodies, prompts, credentials or exception messages.
            LoggerFactory.getLogger(getClass()).warn("image_generation provider=cloudflare outcome=failure type={}",
                    failure.getClass().getSimpleName());
            throw new AiProviderException("Image generation failed. Please try again.");
        }
    }
}
