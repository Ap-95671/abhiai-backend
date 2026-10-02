package com.abhiai.abhiai_backend.ai.orchestration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import com.abhiai.abhiai_backend.ai.AiCompletion;
import com.abhiai.abhiai_backend.ai.pipeline.Intent;

/** Content-free aggregate statistics. Production uses PostgreSQL; isolated tests can use the bounded local store. */
@Service
@org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization
public class RoutingMetrics {
    public record Key(String provider, String model, Intent intent, TaskType task, ExecutionStrategy strategy) { }
    public record Stats(long attempts, long successes, long latencyMs, long inputTokens, long outputTokens,
                        long positive, long negative, long fallbacks) {
        public static Stats empty() { return new Stats(0,0,0,0,0,0,0,0); }
        public Stats plus(Stats b) { return new Stats(attempts+b.attempts, successes+b.successes, latencyMs+b.latencyMs,
                inputTokens+b.inputTokens, outputTokens+b.outputTokens, positive+b.positive, negative+b.negative, fallbacks+b.fallbacks); }
    }
    private final RoutingMetricsStore store;
    private final java.util.concurrent.Executor executor;
    private final Map<Key, Stats> local = new ConcurrentHashMap<>();
    public RoutingMetrics() { store = null; executor = Runnable::run; }
    @Autowired public RoutingMetrics(RoutingMetricsStore store,
            @org.springframework.beans.factory.annotation.Qualifier("aiMetricsExecutor") java.util.concurrent.Executor executor) {
        this.store = store; this.executor = executor;
        try { local.putAll(store.load()); }
        catch (RuntimeException unavailable) { warn("restore"); }
    }
    private void merge(Key key, Stats increment) {
        synchronized (local) {
            if (local.containsKey(key) || local.size() < 2000) local.merge(key, increment, Stats::plus);
        }
    }
    private void warn(String action) {
        org.slf4j.LoggerFactory.getLogger(getClass()).warn("ai_metrics_unavailable requestId={} action={}", org.slf4j.MDC.get("requestId"), action);
    }

    public void record(Key key, boolean success, long latency, AiCompletion completion, boolean fallback) {
        Stats increment = new Stats(1, success ? 1 : 0, Math.max(0, latency),
                completion == null || completion.inputTokens() == null ? 0 : completion.inputTokens(),
                completion == null || completion.outputTokens() == null ? 0 : completion.outputTokens(), 0, 0, fallback ? 1 : 0);
        merge(key, increment);
        if (store != null) {
            String requestId = org.slf4j.MDC.get("requestId");
            try { executor.execute(() -> {
                if (requestId != null) org.slf4j.MDC.put("requestId", requestId);
                try { store.record(key, increment); }
                catch (RuntimeException unavailable) { warn("write"); }
                finally { org.slf4j.MDC.remove("requestId"); }
            }); } catch (java.util.concurrent.RejectedExecutionException overloaded) { warn("queue_full"); }
        }
    }
    public void feedback(Key key, Boolean previous, boolean positive) {
        if (previous != null && previous == positive) return;
        long up = (positive ? 1 : 0) - (Boolean.TRUE.equals(previous) ? 1 : 0);
        long down = (positive ? 0 : 1) - (Boolean.FALSE.equals(previous) ? 1 : 0);
        if (store != null) store.feedback(key, up, down);
        Runnable update = () -> merge(key, new Stats(0,0,0,0,0,up,down,0));
        if (store != null && org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive())
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization() {
                @Override public void afterCommit() { update.run(); }
            });
        else update.run();
    }
    public Stats snapshot(String provider, String model, TaskType task) {
        return local.entrySet().stream().filter(e -> e.getKey().provider().equals(provider)
                && e.getKey().model().equals(model) && e.getKey().task() == task)
                .map(Map.Entry::getValue).reduce(Stats.empty(), Stats::plus);
    }
    public double adjustment(AiModelDefinition model, TaskType task) {
        Stats stats = snapshot(model.provider(), model.providerModelId(), task);
        double confidence = Math.min(1, stats.attempts() / 10.0);
        double success = (stats.successes() + 3.0) / (stats.attempts() + 4.0) - .75;
        double latency = stats.attempts() == 0 ? 0 : 2 / (2 + stats.latencyMs() / (1000.0 * stats.attempts())) - .5;
        long votes = stats.positive() + stats.negative();
        double feedback = ((stats.positive() + 1.0) / (votes + 2.0) - .5) * Math.min(1, votes / 5.0);
        return confidence * (.5 * success + .2 * latency) + .3 * feedback;
    }
}
