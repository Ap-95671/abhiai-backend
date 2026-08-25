package com.abhiai.abhiai_backend.ai.orchestration;

import java.util.EnumSet;
import java.util.Locale;

import org.springframework.stereotype.Component;

import com.abhiai.abhiai_backend.ai.AiChatRequest;

@Component
public class TaskClassifier {

    public TaskClassification classify(AiChatRequest request) {
        String text = request.messages().isEmpty() ? "" : request.messages().getLast().content().toLowerCase(Locale.ROOT);
        var capabilities = EnumSet.of(ModelCapability.TEXT);
        TaskType type = TaskType.GENERAL;
        if (!request.attachments().isEmpty()) { type = TaskType.VISION; capabilities.add(ModelCapability.VISION); }
        else if (contains(text, "code", "debug", "java", "python", "typescript", "sql", "api")) { type = TaskType.CODE; capabilities.add(ModelCapability.CODE); }
        else if (contains(text, "prove", "reason", "analyze", "compare", "architecture", "step by step")) { type = TaskType.REASONING; capabilities.add(ModelCapability.REASONING); }
        else if (contains(text, "summarize", "summary", "tl;dr")) type = TaskType.SUMMARIZATION;
        else if (contains(text, "write a story", "poem", "creative", "brainstorm")) type = TaskType.CREATIVE;
        else if (contains(text, "latest", "research", "sources", "search the web")) { type = TaskType.RESEARCH; capabilities.add(ModelCapability.TOOLS); }
        int length = text.length();
        RequestComplexity complexity = length > 4000 || capabilities.contains(ModelCapability.REASONING)
                ? RequestComplexity.HIGH : length > 800 ? RequestComplexity.MEDIUM : RequestComplexity.LOW;
        return new TaskClassification(type, complexity, capabilities);
    }

    private boolean contains(String text, String... needles) {
        for (String needle : needles) if (text.contains(needle)) return true;
        return false;
    }
}
