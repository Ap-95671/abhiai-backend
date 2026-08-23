package com.abhiai.abhiai_backend.ai;

import java.util.function.Consumer;

public interface AiProvider {

    default String providerName(){return "unknown";}
    default String modelName(){return "unknown";}
    default boolean configured(){return true;}
    default boolean supportsImageUnderstanding(){return false;}

    AiCompletion generate(AiChatRequest request);

    default AiCompletion generateStream(AiChatRequest request, Consumer<String> onTextChunk) {
        AiCompletion completion = generate(request);
        onTextChunk.accept(completion.content());
        return completion;
    }
}
