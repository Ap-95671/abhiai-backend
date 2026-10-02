package com.abhiai.abhiai_backend.ai.pipeline;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import com.abhiai.abhiai_backend.ai.*;
import com.abhiai.abhiai_backend.ai.orchestration.*;
import com.abhiai.abhiai_backend.config.*;
import com.abhiai.abhiai_backend.entity.MessageRole;
import com.abhiai.abhiai_backend.service.AiConversationContextBuilder;
import tools.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.*;

class QualityAndTraceTest {
    private AiRequestProcessor processor(){return new AiRequestProcessor(new IntentClassifier(),new TaskClassifier(),new CapabilityRouter(),new AiConversationContextBuilder(new AiContextProperties()));}
    private AiRequestProcessor.Request request(String text){return processor().process(new AiChatRequest(List.of(new AiChatMessage(MessageRole.USER,text))));}
    private ExecutionPlan plan(){return new ExecutionPlan(ExecutionStrategy.SINGLE_MODEL,List.of(),null,java.time.Duration.ofSeconds(20),java.time.Duration.ofSeconds(90),true);}
    @Test void trivialTasksSkipEvaluationAndComplexMissingConstraintsAreDetected(){
        var evaluator=new ResponseQualityEvaluator(new AiOrchestrationProperties(),new ObjectMapper());
        assertThat(evaluator.evaluate(request("Hello"),plan(),"Hello")).isNull();
        var result=evaluator.evaluate(request("Review complex architecture for security and performance. JSON only"),plan(),"```draft");
        assertThat(result.complete()).isFalse();
        assertThat(result.missingSections()).contains("security","performance");
        assertThat(result.constraintViolations()).contains("UNCLOSED_CODE_BLOCK","INVALID_JSON");
    }
    @Test void evaluatorIsOptionalAndNeverMakesCorrectionCalls(){
        var config=new AiOrchestrationProperties();config.setQualityEvaluationEnabled(false);
        assertThat(new ResponseQualityEvaluator(config,new ObjectMapper()).evaluate(request("Review complex architecture"),plan(),"bad")).isNull();
        config.setQualityEvaluationEnabled(true);
        var result=new ResponseQualityEvaluator(config,new ObjectMapper()).evaluate(request("Review complex architecture for security"),plan(),
                "Security boundaries require isolation, least privilege, encryption, request validation and auditing. Explicitly document assumptions and failure cases.");
        assertThat(result.complete()).isTrue();assertThat(result.qualityScore()).isEqualTo(100);
    }
    @Test void originalCorrelationIdSurvivesNormalizationContextAndStageCopies(){
        MDC.put("requestId","existing-request-123");
        try(var scope=RequestCorrelationFilter.scope()){
            var input=new AiChatRequest(List.of(new AiChatMessage(MessageRole.USER,"Hello")));
            var copy=input.withMessages(input.messages()).withOriginalMessage("Hello").withProviderModelId("model");
            assertThat(processor().process(copy).requestId()).isEqualTo("existing-request-123");
            var staged=new ResponseSynthesizer(new ObjectMapper()).prepare(copy,ModelRole.PRIMARY_SOLVER,List.of());
            assertThat(staged.executionContext().requestId()).isEqualTo("existing-request-123");
        }finally{MDC.clear();}
    }
    @Test void serviceScopeCreatesAndCleansOneId(){
        MDC.clear();String id;
        try(var outer=RequestCorrelationFilter.scope()){
            id=RequestCorrelationFilter.currentId();
            try(var inner=RequestCorrelationFilter.scope()){assertThat(RequestCorrelationFilter.currentId()).isEqualTo(id);}
            assertThat(MDC.get("requestId")).isEqualTo(id);
        }
        assertThat(MDC.get("requestId")).isNull();
    }
}
