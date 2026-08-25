package com.abhiai.abhiai_backend.ai.image;

public interface ImageGenerationProvider {

    String providerName();

    String modelName();

    boolean configured();

    GeneratedImage generate(String prompt);
}
