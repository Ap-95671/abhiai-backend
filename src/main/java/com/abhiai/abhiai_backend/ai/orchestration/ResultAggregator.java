package com.abhiai.abhiai_backend.ai.orchestration;

import java.util.LinkedHashMap;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ResultAggregator {
    public record Evidence(ModelRole role, String content, StageResult.Status status) {
        public Evidence(ModelRole role, String content) { this(role, content, StageResult.Status.SUCCESS); }
    }
    public List<Evidence> forStage(List<StageResult> results) {
        var evidence = new java.util.ArrayList<>(collect(results));
        results.stream().filter(r -> !r.successful()).forEach(r -> evidence.add(new Evidence(r.assignment().role(), "Contribution unavailable", r.status())));
        return List.copyOf(evidence);
    }

    /** Mechanical quality checks only; semantic relevance, agreement and conflict are assessed during synthesis. */
    public List<Evidence> collect(List<StageResult> results) {
        var unique = new LinkedHashMap<String, Evidence>();
        for (var result : results) {
            if (!result.successful()) continue;
            String content = result.completion().content().strip();
            if (content.isBlank()) continue;
            String key = content.replaceAll("\\s+", " ");
            unique.putIfAbsent(key, new Evidence(result.assignment().role(),
                    content.length() > 4000 ? content.substring(0, 4000) + " [excerpt truncated]" : content));
        }
        return List.copyOf(unique.values());
    }
}
