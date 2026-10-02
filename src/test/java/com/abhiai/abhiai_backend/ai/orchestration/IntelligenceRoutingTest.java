package com.abhiai.abhiai_backend.ai.orchestration;

import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import com.abhiai.abhiai_backend.ai.AiCompletion;
import com.abhiai.abhiai_backend.ai.pipeline.*;
import com.abhiai.abhiai_backend.config.*;
import com.abhiai.abhiai_backend.exception.*;
import static org.assertj.core.api.Assertions.*;
import static com.abhiai.abhiai_backend.ai.orchestration.PipelineTestSupport.*;

class IntelligenceRoutingTest {
    @Test void registryOverridesExcludeDisabledAndUnsupportedModels() {
        var registry=registry(); var config=new MultiProviderProperties();
        var disabled=new MultiProviderProperties.Provider(); disabled.setEnabled(false);
        var limited=new MultiProviderProperties.Provider(); limited.setCapabilities(Set.of(ModelCapability.TEXT));
        config.setConfigs(Map.of("openai",disabled,"gemini",limited)); registry.configure(config);
        var router=new ModelRouter(registry,new TaskClassifier(),new ProviderHealthTracker());
        var available=Map.of("openai",provider("openai"),"gemini",provider("gemini"),"groq",provider("groq"));
        assertThat(router.route(request("Debug Java code"),available).candidates()).extracting(AiModelDefinition::provider).containsExactly("groq");
        assertThat(registry.find("gemini:gemini-test").orElseThrow().supportsStreaming()).isFalse();
        assertThat(registry.find("groq:groq-test").orElseThrow().relativeCost()).isEqualTo(AiModelDefinition.CostClass.LOW);
        assertThat(registry.find("groq:groq-test").orElseThrow().capabilities()).doesNotContain(ModelCapability.TOOLS);
    }
    @Test void circuitOpensThenPermitsOnlyOneRecoveryProbe() {
        var clock=new MutableClock(); var health=new ProviderHealthTracker(clock);
        assertThat(health.status("openai",true)).isEqualTo(ModelStatus.AVAILABLE);
        health.failure("openai"); assertThat(health.status("openai",true)).isEqualTo(ModelStatus.DEGRADED);
        health.failure("openai"); health.failure("openai");
        assertThat(health.circuit("openai")).isEqualTo(ProviderHealthTracker.Circuit.OPEN);
        assertThat(health.tryAcquire("openai")).isFalse();
        clock.advance(46);
        assertThat(health.canAttempt("openai")).isTrue();
        assertThat(health.tryAcquire("openai")).isTrue();
        assertThat(health.tryAcquire("openai")).isFalse();
        health.success("openai");
        assertThat(health.circuit("openai")).isEqualTo(ProviderHealthTracker.Circuit.CLOSED);
        assertThat(health.tryAcquire("openai")).isTrue();
    }
    @Test void failedProbeReopensAndAuthenticationHasLongerCooldown() {
        var clock=new MutableClock(); var health=new ProviderHealthTracker(clock);
        health.failure("a",new AiProviderException("quota",AiProviderFailureKind.RATE_LIMIT));
        clock.advance(46); assertThat(health.tryAcquire("a")).isTrue(); health.failure("a");
        assertThat(health.tryAcquire("a")).isFalse();
        health.failure("b",new AiProviderException("key",AiProviderFailureKind.AUTHENTICATION));
        clock.advance(60); assertThat(health.tryAcquire("b")).isFalse();
        clock.advance(841); assertThat(health.tryAcquire("b")).isTrue();
        assertThat(health.tryAcquire("b")).isFalse();
    }
    @Test void historicalLatencyAndSuccessInfluenceScoresAfterCapabilityFiltering() {
        var metrics=new RoutingMetrics(); var model=registry().find("openai:gpt-test").orElseThrow();
        var good=new RoutingMetrics.Key("openai","gpt-test",Intent.CODING,TaskType.CODE,ExecutionStrategy.SINGLE_MODEL);
        for(int i=0;i<20;i++)metrics.record(good,true,100,new AiCompletion("ok"),false);
        double fast=metrics.adjustment(model,TaskType.CODE);
        var slow=new RoutingMetrics();
        for(int i=0;i<20;i++)slow.record(good,true,30000,new AiCompletion("ok"),false);
        assertThat(fast).isGreaterThan(slow.adjustment(model,TaskType.CODE));
        var failing=new RoutingMetrics();
        for(int i=0;i<20;i++)failing.record(good,false,100,null,false);
        assertThat(fast).isGreaterThan(failing.adjustment(model,TaskType.CODE));
        assertThat(metrics.adjustment(model,TaskType.GENERAL)).isZero();
    }
    @Test void feedbackIsReversibleAndInfluencesActualRouterTie() {
        var registry=registry(); var properties=new MultiProviderProperties();
        var equal=new MultiProviderProperties.Provider(); equal.setQualityScore(.8);equal.setSpeedScore(.8);equal.setCostScore(.8);
        properties.setConfigs(Map.of("openai",equal,"gemini",equal));registry.configure(properties);
        var metrics=new RoutingMetrics(); var config=new AiOrchestrationProperties();
        var router=new ModelRouter(registry,new TaskClassifier(),new ProviderHealthTracker());router.configureAdaptive(metrics,config);
        var key=new RoutingMetrics.Key("gemini","gemini-test",Intent.GENERAL_CHAT,TaskType.GENERAL,ExecutionStrategy.SINGLE_MODEL);
        for(int i=0;i<10;i++)metrics.feedback(key,null,true);
        var providers=Map.of("openai",provider("openai"),"gemini",provider("gemini"));
        assertThat(router.route(request("Hello"),providers).candidates().getFirst().provider()).isEqualTo("gemini");
        metrics.feedback(key,true,false);
        metrics.feedback(key,false,false);
        assertThat(metrics.snapshot("gemini","gemini-test",TaskType.GENERAL).positive()).isEqualTo(9);
        assertThat(metrics.snapshot("gemini","gemini-test",TaskType.GENERAL).negative()).isEqualTo(1);
    }
    @Test void profilesAndBudgetsBoundModelsStagesAndCost() {
        var config=new AiOrchestrationProperties();var planner=new ExecutionPlanner(config);
        var models=registry().all().stream().filter(m->List.of("openai","anthropic","gemini").contains(m.provider())).toList();
        var complex=processor().process(request("Design scalable distributed architecture and review security, database consistency and failure handling"));
        config.setProfile(AiOrchestrationProperties.ExecutionProfile.FAST);
        assertThat(planner.plan(complex,models).strategy()).isEqualTo(ExecutionStrategy.SINGLE_MODEL);
        config.setProfile(AiOrchestrationProperties.ExecutionProfile.BALANCED);
        assertThat(planner.plan(complex,models).assignments()).hasSize(2);
        config.setProfile(AiOrchestrationProperties.ExecutionProfile.HIGH);
        assertThat(planner.plan(complex,models).assignments()).hasSize(3);
        config.setMaxStages(3);assertThat(planner.plan(complex,models).assignments()).hasSize(2);
        config.setMaxRelativeCostUnits(3);assertThat(planner.plan(complex,models).strategy()).isEqualTo(ExecutionStrategy.SINGLE_MODEL);
        assertThat(planner.plan(processor().process(request("What is binary search?")),models).strategy()).isEqualTo(ExecutionStrategy.SINGLE_MODEL);
    }
    @Test void unavailableProviderIsExcludedEvenWithExcellentMetrics() {
        var health=new ProviderHealthTracker();health.failure("openai",new AiProviderException("rate",AiProviderFailureKind.RATE_LIMIT));
        var router=new ModelRouter(registry(),new TaskClassifier(),health);
        assertThat(router.route(request("Hello"),Map.of("openai",provider("openai"),"gemini",provider("gemini"))).candidates())
                .extracting(AiModelDefinition::provider).containsExactly("gemini");
    }
    @Test void metricWritesAreDeferredOffTheProviderPath() {
        var store=org.mockito.Mockito.mock(RoutingMetricsStore.class);
        org.mockito.Mockito.when(store.load()).thenReturn(Map.of());
        var queued=new ArrayList<Runnable>();var metrics=new RoutingMetrics(store,queued::add);
        var key=new RoutingMetrics.Key("openai","model",Intent.CODING,TaskType.CODE,ExecutionStrategy.SINGLE_MODEL);
        metrics.record(key,true,12,new AiCompletion("answer"),false);
        assertThat(metrics.snapshot("openai","model",TaskType.CODE).attempts()).isEqualTo(1);
        org.mockito.Mockito.verify(store,org.mockito.Mockito.never()).record(org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any());
        assertThat(queued).hasSize(1);queued.getFirst().run();
        org.mockito.Mockito.verify(store).record(org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any());
    }

    static class MutableClock extends Clock {
        private Instant now=Instant.parse("2026-10-02T00:00:00Z");
        void advance(long seconds){now=now.plusSeconds(seconds);}
        public ZoneId getZone(){return ZoneOffset.UTC;}
        public Clock withZone(ZoneId zone){return this;}
        public Instant instant(){return now;}
    }
}
