package com.abhiai.abhiai_backend.ai.orchestration;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.dao.DataAccessException;
import org.slf4j.LoggerFactory;
import com.abhiai.abhiai_backend.ai.orchestration.RoutingMetrics.*;

@Repository
public class RoutingMetricsStore {
    private final JdbcTemplate jdbc;
    public RoutingMetricsStore(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    private static final String UPSERT = """
        INSERT INTO ai_routing_metrics(provider, model, intent, task_type, strategy, attempts, successes, latency_ms,
            input_tokens, output_tokens, positive_feedback, negative_feedback, fallbacks)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT (provider, model, intent, task_type, strategy) DO UPDATE SET
            attempts = ai_routing_metrics.attempts + EXCLUDED.attempts,
            successes = ai_routing_metrics.successes + EXCLUDED.successes,
            latency_ms = ai_routing_metrics.latency_ms + EXCLUDED.latency_ms,
            input_tokens = ai_routing_metrics.input_tokens + EXCLUDED.input_tokens,
            output_tokens = ai_routing_metrics.output_tokens + EXCLUDED.output_tokens,
            positive_feedback = GREATEST(0, ai_routing_metrics.positive_feedback + EXCLUDED.positive_feedback),
            negative_feedback = GREATEST(0, ai_routing_metrics.negative_feedback + EXCLUDED.negative_feedback),
            fallbacks = ai_routing_metrics.fallbacks + EXCLUDED.fallbacks
        """;
    // Provider usage happened even if conversation persistence later rolls back.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(Key key, Stats increment) {
        try { update(key, increment); }
        catch (DataAccessException unavailable) {
            LoggerFactory.getLogger(getClass()).warn("ai_metrics_write_unavailable requestId={}", org.slf4j.MDC.get("requestId"));
        }
    }
    // Feedback changes and message feedback are committed atomically by ChatService's transaction.
    public void feedback(Key key, long positive, long negative) { update(key, new Stats(0,0,0,0,0,positive,negative,0)); }
    private void update(Key k, Stats s) {
        jdbc.update(UPSERT, k.provider(), k.model(), k.intent().name(), k.task().name(), k.strategy().name(), s.attempts(),
                s.successes(), s.latencyMs(), s.inputTokens(), s.outputTokens(), s.positive(), s.negative(), s.fallbacks());
    }
    public java.util.Map<Key, Stats> load() {
        var restored = new java.util.HashMap<Key, Stats>();
        jdbc.query("SELECT * FROM ai_routing_metrics ORDER BY attempts DESC LIMIT 2000", (org.springframework.jdbc.core.RowCallbackHandler) rs -> {
            var key = new Key(rs.getString("provider"), rs.getString("model"), com.abhiai.abhiai_backend.ai.pipeline.Intent.valueOf(rs.getString("intent")),
                    TaskType.valueOf(rs.getString("task_type")), ExecutionStrategy.valueOf(rs.getString("strategy")));
            restored.put(key, new Stats(rs.getLong("attempts"), rs.getLong("successes"), rs.getLong("latency_ms"),
                    rs.getLong("input_tokens"), rs.getLong("output_tokens"), rs.getLong("positive_feedback"), rs.getLong("negative_feedback"), rs.getLong("fallbacks")));
        });
        return restored;
    }
}
