package com.abhiai.abhiai_backend.ai.orchestration;

import java.util.EnumSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.abhiai.abhiai_backend.ai.AiChatRequest;
import com.abhiai.abhiai_backend.ai.pipeline.IntentClassifier;
import com.abhiai.abhiai_backend.ai.pipeline.TaskAnalyzer;

/** Existing classification entry point, now backed by separate intent and complexity analysis. */
@Component
public class TaskClassifier {
    private final IntentClassifier intents;
    private final TaskAnalyzer analyzer;

    public TaskClassifier() { this(new IntentClassifier(), new TaskAnalyzer()); }
    @Autowired
    public TaskClassifier(IntentClassifier intents, TaskAnalyzer analyzer) { this.intents = intents; this.analyzer = analyzer; }

    public TaskClassification classify(AiChatRequest request) {
        var intent = intents.classify(request.originalMessage());
        var capabilities = EnumSet.of(ModelCapability.TEXT);
        TaskType type = switch (intent) {
            case CODING -> TaskType.CODE;
            case REASONING, DATA_ANALYSIS, PLANNING -> TaskType.REASONING;
            case CREATIVE_WRITING -> TaskType.CREATIVE;
            case SUMMARIZATION, DOCUMENT_ANALYSIS -> TaskType.SUMMARIZATION;
            case CURRENT_INFORMATION -> TaskType.RESEARCH;
            default -> TaskType.GENERAL;
        };
        if (type == TaskType.CODE) capabilities.add(ModelCapability.CODE);
        if (type == TaskType.REASONING) capabilities.add(ModelCapability.REASONING);
        if (!request.attachments().isEmpty()) { capabilities.add(ModelCapability.VISION); type = TaskType.VISION; }
        // Web search is performed by the existing opt-in tool registry, not fabricated by model routing.
        int characters = request.messages().stream().mapToInt(m -> m.content().length()).sum();
        return new TaskClassification(type, analyzer.analyze(intent, request.originalMessage(), characters), capabilities);
    }
}
