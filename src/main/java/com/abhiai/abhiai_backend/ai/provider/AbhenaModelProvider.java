package com.abhiai.abhiai_backend.ai.provider;

import org.springframework.stereotype.Component;

import com.abhiai.abhiai_backend.ai.AiChatRequest;
import com.abhiai.abhiai_backend.ai.AiCompletion;
import com.abhiai.abhiai_backend.ai.ModelProvider;
import com.abhiai.abhiai_backend.exception.AiProviderUnavailableException;

/** Stable extension point for AbhiAI's future native model. */
@Component
public class AbhenaModelProvider implements ModelProvider {
    @Override public String providerName() { return "abhena"; }
    @Override public String modelName() { return "abhena-preview"; }
    @Override public boolean configured() { return false; }
    @Override public AiCompletion generate(AiChatRequest request) { throw new AiProviderUnavailableException(); }
}
