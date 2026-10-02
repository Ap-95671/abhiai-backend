package com.abhiai.abhiai_backend.ai.pipeline;

import org.springframework.stereotype.Component;

@Component
public class CapabilityRouter {
    public Capability route(Intent intent) {
        return switch (intent) {
            case IMAGE_GENERATION -> Capability.IMAGE_GENERATION;
            case CURRENT_INFORMATION -> Capability.WEB_SEARCH;
            case CODING -> Capability.CODE_REASONING;
            case DOCUMENT_ANALYSIS -> Capability.DOCUMENT_ANALYSIS;
            default -> Capability.TEXT_GENERATION;
        };
    }
}
