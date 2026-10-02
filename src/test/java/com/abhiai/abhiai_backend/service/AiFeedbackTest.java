package com.abhiai.abhiai_backend.service;

import java.util.*;
import org.junit.jupiter.api.Test;
import com.abhiai.abhiai_backend.ai.*;
import com.abhiai.abhiai_backend.ai.pipeline.*;
import com.abhiai.abhiai_backend.ai.orchestration.*;
import com.abhiai.abhiai_backend.entity.*;
import com.abhiai.abhiai_backend.repository.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AiFeedbackTest {
    @Test void feedbackRequiresOwnershipAndIsIdempotentAndReversible(){
        var repo=mock(MessageRepository.class);
        var service=new ChatService(mock(ConversationRepository.class),repo,mock(UserRepository.class),mock(AiProvider.class));
        var metrics=new RoutingMetrics();service.setRoutingMetrics(metrics);
        UUID user=UUID.randomUUID(),conversation=UUID.randomUUID(),id=UUID.randomUUID();
        var message=new Message(new Conversation(new User("u","U","e@example.com","hash"),"Chat"),MessageRole.ASSISTANT,"Answer");
        message.applyAiMetadata(new AiCompletion("Answer","openai","model",null,1,2,3,false)
                .withExecution(new AiExecutionMetadata("trace",Intent.CODING,TaskType.CODE,RequestComplexity.HIGH,ExecutionStrategy.SINGLE_MODEL,90)));
        when(repo.findOwnedForFeedback(id,conversation,user)).thenReturn(Optional.of(message));
        service.recordFeedback(user,conversation,id,true);service.recordFeedback(user,conversation,id,true);
        assertThat(metrics.snapshot("openai","model",TaskType.CODE).positive()).isEqualTo(1);
        service.recordFeedback(user,conversation,id,false);
        assertThat(metrics.snapshot("openai","model",TaskType.CODE).positive()).isZero();
        assertThat(metrics.snapshot("openai","model",TaskType.CODE).negative()).isEqualTo(1);
        assertThatThrownBy(()->service.recordFeedback(UUID.randomUUID(),conversation,id,true))
                .isInstanceOf(com.abhiai.abhiai_backend.exception.ConversationNotFoundException.class);
        assertThat(message.getAiFeedback()).isFalse();
        var json=new tools.jackson.databind.ObjectMapper().valueToTree(com.abhiai.abhiai_backend.dto.chat.MessageResponse.from(message));
        assertThat(json.has("aiRequestId")).isFalse();assertThat(json.has("aiQualityScore")).isFalse();
    }
}
