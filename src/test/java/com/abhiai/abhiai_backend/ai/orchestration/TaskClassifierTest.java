package com.abhiai.abhiai_backend.ai.orchestration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.abhiai.abhiai_backend.ai.AiChatMessage;
import com.abhiai.abhiai_backend.ai.AiChatRequest;
import com.abhiai.abhiai_backend.entity.MessageRole;

class TaskClassifierTest {
    private final TaskClassifier classifier = new TaskClassifier();

    @Test void classifiesCodeRequestsWithoutCallingAnExternalModel() {
        var result = classifier.classify(new AiChatRequest(List.of(new AiChatMessage(MessageRole.USER, "Debug this Java API"))));
        assertThat(result.taskType()).isEqualTo(TaskType.CODE);
        assertThat(result.requiredCapabilities()).contains(ModelCapability.CODE);
    }

    @Test void marksReasoningAsHighComplexity() {
        var result = classifier.classify(new AiChatRequest(List.of(new AiChatMessage(MessageRole.USER, "Analyze this architecture step by step"))));
        assertThat(result.taskType()).isEqualTo(TaskType.REASONING);
        assertThat(result.complexity()).isEqualTo(RequestComplexity.HIGH);
    }
}
