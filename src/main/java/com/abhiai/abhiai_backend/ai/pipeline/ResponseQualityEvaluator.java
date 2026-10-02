package com.abhiai.abhiai_backend.ai.pipeline;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import com.abhiai.abhiai_backend.ai.orchestration.*;
import com.abhiai.abhiai_backend.config.AiOrchestrationProperties;
import tools.jackson.databind.ObjectMapper;
import static com.abhiai.abhiai_backend.ai.pipeline.IntentClassifier.has;

/** Optional deterministic structural review. No model calls, regeneration or factual truth scoring. */
@Component
public class ResponseQualityEvaluator {
    private final AiOrchestrationProperties properties;
    private final ObjectMapper mapper;
    public ResponseQualityEvaluator(AiOrchestrationProperties properties, ObjectMapper mapper) { this.properties=properties; this.mapper=mapper; }
    public record Evaluation(boolean complete, List<String> constraintViolations, List<String> missingSections, int qualityScore) { }
    public Evaluation evaluate(AiRequestProcessor.Request request, ExecutionPlan plan, String answer) {
        if (!properties.isQualityEvaluationEnabled() || !plan.multiModel() && request.analysis().complexity().ordinal() < RequestComplexity.HIGH.ordinal()) return null;
        var violations = new ArrayList<String>();
        var missing = new ArrayList<String>();
        if (answer.strip().length() < 80) violations.add("RESPONSE_TOO_SHORT_FOR_COMPLEX_TASK");
        if ((answer.split("```", -1).length - 1) % 2 != 0) violations.add("UNCLOSED_CODE_BLOCK");
        String original = request.input().originalMessage();
        if (has(original, "json only|only json")) {
            try { mapper.readTree(answer); } catch (RuntimeException invalid) { violations.add("INVALID_JSON"); }
        }
        for (String topic : List.of("security", "performance", "concurrency", "testing", "alternatives"))
            if (has(original, topic) && !has(answer, topic + (topic.equals("testing") ? "|tests|test" : ""))) missing.add(topic);
        int score = Math.max(0, 100 - violations.size()*25 - missing.size()*10);
        return new Evaluation(violations.isEmpty() && missing.isEmpty(), List.copyOf(violations), List.copyOf(missing), score);
    }
}
