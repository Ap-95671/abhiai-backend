package com.abhiai.abhiai_backend.service;

import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import com.abhiai.abhiai_backend.ai.*;
import com.abhiai.abhiai_backend.ai.image.*;
import com.abhiai.abhiai_backend.ai.orchestration.*;
import com.abhiai.abhiai_backend.ai.pipeline.*;
import com.abhiai.abhiai_backend.config.*;
import com.abhiai.abhiai_backend.dto.chat.SendMessageRequest;
import com.abhiai.abhiai_backend.entity.*;
import com.abhiai.abhiai_backend.repository.*;
import com.abhiai.abhiai_backend.exception.ConversationNotFoundException;
import tools.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Real service/pipeline/router/execution; repositories and external providers are isolated test doubles. */
class AiPipelineChatIntegrationTest {
    private final UUID userId = UUID.randomUUID();
    private final UUID conversationId = UUID.randomUUID();
    private ConversationRepository conversations;
    private MessageRepository messages;
    private ConversationAttachmentRepository attachments;
    private ModelProvider openai;
    private ModelProvider gemini;
    private CloudflareImageGenerationProvider cloudflare;
    private ChatService service;
    private ImageGenerationService images;
    private ExecutorService executor;
    private User user;

    @BeforeEach void setup() {
        conversations = mock(ConversationRepository.class);
        messages = mock(MessageRepository.class);
        attachments = mock(ConversationAttachmentRepository.class);
        var users = mock(UserRepository.class);
        user = new User("user", "User", "user@example.com", "hash");
        var conversation = new Conversation(user, "New conversation");
        when(conversations.findByIdAndUserId(conversationId, userId)).thenReturn(Optional.of(conversation));
        when(messages.findAllByConversationIdOrderByCreatedAtAscIdAsc(conversationId)).thenReturn(List.of());
        when(messages.save(any())).thenAnswer(i -> i.getArgument(0));
        openai = provider("openai"); gemini = provider("gemini");
        var health = new ProviderHealthTracker();
        var classifier = new TaskClassifier();
        var context = new AiConversationContextBuilder(new AiContextProperties());
        var processor = new AiRequestProcessor(new IntentClassifier(), classifier, new CapabilityRouter(), context);
        var planner = new ExecutionPlanner(new AiOrchestrationProperties());
        var registry = new ModelRegistry("gpt-test", "gemini-test", "groq-test", "local-test", "claude-test",
                "grok-test", "deepseek-test", "mistral-test", "cohere-test", "router-test");
        executor = Executors.newFixedThreadPool(3);
        var responses = new ResponseProcessor();
        var multi = new MultiModelOrchestrator(executor, health, new ResultAggregator(), new ResponseSynthesizer(new ObjectMapper()), responses);
        var pipeline = new OrchestratingAiProvider(List.of(openai, gemini), new ModelRouter(registry, classifier, health),
                health, processor, planner, multi, responses);
        service = new ChatService(conversations, messages, users, pipeline, context, null, null, null);
        cloudflare = mock(CloudflareImageGenerationProvider.class);
        var media = mock(MediaService.class);
        when(media.storeGeneratedImage(eq(userId), any(), eq("image/png"))).thenReturn(
                new MediaAsset(UUID.randomUUID(), user, "generated.png", "generated.png", "image/png", 3));
        images = new ImageGenerationService(conversations, messages, attachments, media,
                new RoutingImageGenerationProvider(cloudflare), processor, planner);
        service.setImagePipeline(processor, images);
    }
    @AfterEach void close() throws Exception { executor.shutdownNow(); assertThat(executor.awaitTermination(2, TimeUnit.SECONDS)).isTrue(); }
    private ModelProvider provider(String name) {
        var provider = mock(ModelProvider.class);
        when(provider.providerName()).thenReturn(name);
        when(provider.configured()).thenReturn(true);
        return provider;
    }
    @Test void currentSearchRunsOnceBeforeModelsAndPersistsSourceMetadataInStreaming() {
        var search = mock(com.abhiai.abhiai_backend.ai.tool.WebSearchTool.class);
        var source = new com.abhiai.abhiai_backend.ai.tool.WebSearchSource("Verified source", "https://example.com/news", "Retrieved current facts", "example.com", "2026-10-02", java.time.Instant.parse("2026-10-02T10:00:00Z"));
        when(search.search(any())).thenReturn(new com.abhiai.abhiai_backend.ai.tool.WebSearchResult("[Retrieved web sources] Retrieved current facts https://example.com/news", List.of(source)));
        org.springframework.test.util.ReflectionTestUtils.setField(service, "toolRegistry", new com.abhiai.abhiai_backend.ai.tool.AiToolRegistry(search));
        when(openai.generate(any())).thenAnswer(call -> {
            AiChatRequest request = call.getArgument(0);
            assertThat(request.messages().getLast().content()).contains("Retrieved current facts");
            assertThat(request.executionContext().toolCalls()).containsExactly("web_search");
            assertThat(request.messages()).anyMatch(m -> m.role()==MessageRole.SYSTEM && m.content().contains("not model memory alone"));
            return new AiCompletion(request.messages().stream().anyMatch(m -> m.content().startsWith(ModelRole.SYNTHESIZER.instruction()))
                    ? "Unified current analysis citing https://example.com/news" : "Independent current analysis");
        });
        when(gemini.generate(any())).thenReturn(new AiCompletion("Alternative current analysis"));
        var chunks = new ArrayList<String>();
        var result = service.addUserMessageStreaming(userId, conversationId,
                new SendMessageRequest("Compare multiple approaches for distributed architecture using latest research today"), chunks::add);
        verify(search, times(1)).search(any());
        assertThat(result.assistantMessage().citations()).hasSize(1);
        assertThat(result.assistantMessage().citations().getFirst().description()).isEqualTo("Retrieved current facts");
        assertThat(result.assistantMessage().citations().getFirst().sourceDate()).isEqualTo("2026-10-02");
        assertThat(result.assistantMessage().citations().getFirst().retrievedAt()).isEqualTo(source.retrievedAt());
        assertThat(chunks).containsExactly("Unified current analysis citing https://example.com/news");
    }

    @Test void searchFailureStopsBeforeAnyTextProviderCanFabricateCurrentAnswer() {
        var search=mock(com.abhiai.abhiai_backend.ai.tool.WebSearchTool.class);
        when(search.search(any())).thenThrow(new com.abhiai.abhiai_backend.exception.AiProviderException("Current information could not be verified.", com.abhiai.abhiai_backend.exception.AiProviderFailureKind.SEARCH_FAILURE));
        org.springframework.test.util.ReflectionTestUtils.setField(service, "toolRegistry", new com.abhiai.abhiai_backend.ai.tool.AiToolRegistry(search));
        assertThatThrownBy(() -> service.addUserMessage(userId,conversationId,new SendMessageRequest("What are the latest AI developments today?")))
                .hasMessageContaining("could not be verified");
        verify(openai, never()).generate(any()); verify(gemini, never()).generate(any());
        verify(messages, times(1)).save(any()); // transaction rolls back the user message on the real service proxy
    }

    @Test void normalChatUsesRealPipelineAndPersistsOneExchange() {
        when(gemini.generate(any())).thenReturn(new AiCompletion("Hello back"));
        var exchange = service.addUserMessage(userId, conversationId, new SendMessageRequest("Hello"));
        assertThat(exchange.userMessage().content()).isEqualTo("Hello");
        assertThat(exchange.assistantMessage().content()).isEqualTo("Hello back");
        assertThat(exchange.assistantMessage().provider()).isEqualTo("gemini");
        verify(messages, times(2)).save(any());
        verify(openai, never()).generate(any());
    }
    @Test void complexStreamPersistsAndEmitsOnlyUnifiedResponse() {
        when(openai.generate(any())).thenAnswer(call -> {
            AiChatRequest input = call.getArgument(0);
            return new AiCompletion(input.messages().stream().anyMatch(m -> m.content().startsWith(ModelRole.SYNTHESIZER.instruction()))
                    ? "Unified design" : "Private draft A");
        });
        when(gemini.generate(any())).thenAnswer(call -> {
            AiChatRequest input = call.getArgument(0);
            return new AiCompletion(input.messages().stream().anyMatch(m -> m.content().startsWith(ModelRole.SYNTHESIZER.instruction()))
                    ? "Unified design" : "Private draft B");
        });
        var chunks = new ArrayList<String>();
        var exchange = service.addUserMessageStreaming(userId, conversationId,
                new SendMessageRequest("Compare multiple approaches for distributed architecture"), chunks::add);
        assertThat(chunks).containsExactly("Unified design");
        assertThat(exchange.assistantMessage().content()).isEqualTo("Unified design");
        var saved = ArgumentCaptor.forClass(Message.class);
        verify(messages, times(2)).save(saved.capture());
        assertThat(saved.getAllValues()).extracting(Message::getContent).doesNotContain("Private draft A", "Private draft B");
        assertThat(new ObjectMapper().valueToTree(exchange).propertyNames()).containsExactlyInAnyOrder("userMessage", "assistantMessage", "conversation");
    }
    @Test void normalImagePromptUsesCloudflareAndStoresImageAttachment() {
        when(cloudflare.generate(any())).thenReturn(new GeneratedImage(new byte[]{1, 2, 3}, "image/png", "flux"));
        var exchange = service.addUserMessage(userId, conversationId, new SendMessageRequest("Generate an image of Mars"));
        assertThat(exchange.assistantMessage().attachments()).hasSize(1);
        assertThat(exchange.assistantMessage().attachments().getFirst().kind()).isEqualTo(AiAttachmentKind.IMAGE);
        verify(cloudflare).generate("Generate an image of Mars");
        verify(messages, times(2)).save(any());
        verify(attachments).save(any());
        verify(openai, never()).generate(any());
        verify(gemini, never()).generate(any());
        verify(gemini, never()).generateStream(any(), any());
    }
    @Test void streamingImageKeepsCompleteExchangeContractAndNeverCallsGemini() {
        when(cloudflare.generate(any())).thenReturn(new GeneratedImage(new byte[]{1}, "image/png", "flux"));
        var chunks = new ArrayList<String>();
        var exchange = service.addUserMessageStreaming(userId, conversationId,
                new SendMessageRequest("Create an image of a futuristic university"), chunks::add);
        assertThat(chunks).containsExactly("Generated an image from your description.");
        assertThat(exchange.assistantMessage().attachments()).hasSize(1);
        verify(gemini, never()).generateStream(any(), any());
        verify(gemini, never()).generate(any());
    }
    @Test void explicitImageEndpointAcceptsBarePromptUsingSameService() {
        when(cloudflare.generate(any())).thenReturn(new GeneratedImage(new byte[]{1}, "image/png", "flux"));
        assertThat(images.generate(userId, conversationId, "A futuristic university").assistantMessage().attachments()).hasSize(1);
        verify(cloudflare).generate("A futuristic university");
        verify(gemini, never()).generate(any());
    }
    @Test void ownershipIsCheckedBeforeAnyImageCall() {
        when(conversations.findByIdAndUserId(conversationId, userId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.addUserMessage(userId, conversationId, new SendMessageRequest("Generate an image of Mars")))
                .isInstanceOf(ConversationNotFoundException.class);
        verifyNoInteractions(cloudflare);
        verify(messages, never()).save(any());
    }
}
