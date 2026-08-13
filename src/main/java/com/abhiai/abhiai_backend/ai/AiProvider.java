package com.abhiai.abhiai_backend.ai;

public interface AiProvider {

    AiCompletion generate(AiChatRequest request);
}
