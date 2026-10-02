package com.abhiai.abhiai_backend.ai.orchestration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import com.abhiai.abhiai_backend.ai.*;
import com.abhiai.abhiai_backend.ai.pipeline.*;
import com.abhiai.abhiai_backend.config.AiConfig;
import com.abhiai.abhiai_backend.service.AiConversationContextBuilder;
import tools.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.*;

class AiPipelineWiringTest {
    @Test void springSelectsExistingPrimaryProviderWithRealPipelineAndBoundedExecutor() {
        new ApplicationContextRunner().withBean(ObjectMapper.class, ObjectMapper::new)
                .withBean(RoutingMetrics.class, RoutingMetrics::new)
                .withUserConfiguration(AiConfig.class, IntentClassifier.class, TaskAnalyzer.class, TaskClassifier.class,
                        CapabilityRouter.class, AiRequestProcessor.class, AiConversationContextBuilder.class,
                        ModelRegistry.class, ModelRouter.class, ProviderHealthTracker.class, ExecutionPlanner.class,
                        ResultAggregator.class, ResponseSynthesizer.class, ResponseProcessor.class, ResponseQualityEvaluator.class,
                        MultiModelOrchestrator.class, OrchestratingAiProvider.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(AiProvider.class)).isInstanceOf(OrchestratingAiProvider.class);
                    var executor = context.getBean("aiOrchestrationExecutor", java.util.concurrent.ThreadPoolExecutor.class);
                    assertThat(executor.getMaximumPoolSize()).isEqualTo(6);
                    assertThat(executor.getQueue().remainingCapacity()).isEqualTo(32);
                    assertThat(context.getBean(AiRequestProcessor.class).isImageRequest("Generate an image of Mars")).isTrue();
                });
    }
}
