package com.abhiai.abhiai_backend.ai.pipeline;

import org.springframework.stereotype.Component;
import com.abhiai.abhiai_backend.ai.AiCompletion;
import com.abhiai.abhiai_backend.exception.AiProviderException;

@Component
public class ResponseProcessor {
    public AiCompletion process(AiCompletion completion) {
        if (completion == null || completion.content() == null || completion.content().isBlank())
            throw new AiProviderException("The AI provider returned no assistant message");
        return completion;
    }
}
