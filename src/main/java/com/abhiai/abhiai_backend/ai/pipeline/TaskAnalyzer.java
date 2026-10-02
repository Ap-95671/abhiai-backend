package com.abhiai.abhiai_backend.ai.pipeline;

import org.springframework.stereotype.Component;
import com.abhiai.abhiai_backend.ai.orchestration.RequestComplexity;
import static com.abhiai.abhiai_backend.ai.pipeline.IntentClassifier.has;

@Component
public class TaskAnalyzer {
    public RequestComplexity analyze(Intent intent, String text, int contextCharacters) {
        int concerns = concernCount(text);
        boolean difficult = has(text, "architecture|distributed|complex|scalable|concurrency|synthesis|multi-stage");
        if (difficult && (concerns >= 3 || has(text, "multiple approaches|alternative architectures|multi-stage|research synthesis")))
            return RequestComplexity.VERY_HIGH;
        if (difficult || text.length() > 4000 || contextCharacters > 30000)
            return RequestComplexity.HIGH;
        if (text.length() > 800 || switch (intent) {
            case CODING, REASONING, DOCUMENT_ANALYSIS, SUMMARIZATION, DATA_ANALYSIS, PLANNING -> true;
            default -> false;
        }) return RequestComplexity.MEDIUM;
        return RequestComplexity.LOW;
    }

    public int concernCount(String text) {
        int count = 0;
        for (String concern : new String[]{"security|privacy", "performance|scalable|latency",
                "database|consistency|storage|memory", "failure|reliability|availability", "concurrency|correctness", "implementation|implement"}) {
            if (has(text, concern)) count++;
        }
        return count;
    }
}
