package com.abhiai.abhiai_backend.assistant;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.test.util.ReflectionTestUtils;
import com.abhiai.abhiai_backend.ai.gemini.GeminiProvider;
import com.abhiai.abhiai_backend.service.AiMemoryService;
import com.abhiai.abhiai_backend.repository.UserRepository;
import com.abhiai.abhiai_backend.entity.User;
import tools.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AgentOrchestratorTest {
    UUID user=UUID.randomUUID(),conversation=UUID.randomUUID(),session=UUID.randomUUID();
    AgentTaskRepository tasks=mock(AgentTaskRepository.class);
    AssistantConversationService conversations=mock(AssistantConversationService.class);
    AssistantToolRegistry tools=mock(AssistantToolRegistry.class);
    GeminiProvider planner=mock(GeminiProvider.class);
    AssistantPreferencesService preferences=mock(AssistantPreferencesService.class);
    AssistantConfirmationService confirmations=mock(AssistantConfirmationService.class);
    AiMemoryService memory=mock(AiMemoryService.class);
    AgentLimits limits=new AgentLimits();
    Map<UUID,AgentTask> stored=new ConcurrentHashMap<>();
    AgentOrchestrator agent;
    @BeforeEach void setup(){
        var manager=mock(PlatformTransactionManager.class);when(manager.getTransaction(any())).thenAnswer(i->new SimpleTransactionStatus());
        when(tasks.save(any())).thenAnswer(i->{AgentTask t=i.getArgument(0);stored.put(t.id,t);return t;});
        when(tasks.lock(any(),any())).thenAnswer(i->Optional.ofNullable(stored.get(i.getArgument(0))).filter(t->t.userId.equals(i.getArgument(1))));
        when(tasks.findByIdAndUserId(any(),any())).thenAnswer(i->Optional.ofNullable(stored.get(i.getArgument(0))).filter(t->t.userId.equals(i.getArgument(1))));
        when(preferences.get(user)).thenReturn(new AssistantPreferencesService.Settings("STANDARD",true,false,false,"",false));
        when(tools.environment(eq(user),any(),anyString())).thenReturn(Map.of("page",Map.of("available",false)));
        when(memory.relevant(any(),any(),any(),any(),any())).thenReturn(List.of());
        when(tools.execute(eq(user),anyString(),anyMap(),any())).thenAnswer(i->new AssistantToolRegistry.Result(i.getArgument(1),"cards","Sources","Verified source",List.of(),null,null,"neutral"));
        agent=new AgentOrchestrator(tasks,conversations,tools,planner,preferences,confirmations,memory,limits,new ObjectMapper(),manager);
        var users=mock(UserRepository.class);when(users.lockAssistantOwner(user)).thenReturn(Optional.of(new User("tester","Test","t@example.com","hash")));
        ReflectionTestUtils.setField(agent,"users",users);
    }
    @AfterEach void cleanup(){agent.close();}
    String decision(String tool,String value){return new ObjectMapper().writeValueAsString(Map.of("plan",List.of("Find sources","Prepare result"),"complete",false,"tool",tool,"value",value,"result",""));}
    String done(){return "{\"plan\":[\"Done\"],\"complete\":true,\"tool\":\"\",\"value\":\"\",\"result\":\"Prepared your draft from the supplied sources.\"}";}
    AgentOrchestrator.View create(){return agent.create(user,conversation,"Find three AI articles and prepare a post",null,session);}
    @Test void multiStepUsesPreviousResultsAndPersistsSharedConversationWithoutPublishing(){
        when(planner.generateStructured(any(),anyMap())).thenReturn(decision("SEARCH_NEWS","AI"),decision("CREATE_POST_DRAFT","My grounded draft"),done());
        var t=create();t=agent.advance(user,t.id());assertEquals(1,t.state().steps().size());
        t=agent.advance(user,t.id());assertEquals(2,t.state().steps().size());
        t=agent.advance(user,t.id());assertEquals("COMPLETED",t.status());assertEquals(2,t.toolCalls());
        verify(conversations,times(2)).append(eq(user),eq(conversation),any());verifyNoInteractions(confirmations);
        var requests=org.mockito.ArgumentCaptor.forClass(com.abhiai.abhiai_backend.ai.AiChatRequest.class);
        verify(planner,times(3)).generateStructured(requests.capture(),anyMap());
        assertTrue(requests.getAllValues().get(1).messages().stream().anyMatch(m->m.content().contains("Verified source")));
    }
    @Test void retryPreservesCompletedStepsAndRevalidatesAccess(){
        when(planner.generateStructured(any(),anyMap())).thenReturn(decision("SEARCH_NEWS","AI")).thenThrow(new IllegalStateException()).thenReturn(done());
        var t=create();t=agent.advance(user,t.id());t=agent.advance(user,t.id());assertEquals("FAILED",t.status());assertEquals(1,t.state().steps().size());
        t=agent.resume(user,t.id(),session);t=agent.advance(user,t.id());assertEquals("COMPLETED",t.status());
        verify(tools,times(1)).execute(user,"SEARCH_NEWS",Map.of("query","AI"),null);
        verify(tools,atLeastOnce()).revalidate(eq(user),any(),any());verify(confirmations).invalidate(t.id());
    }
    @Test void cancellationDiscardsLateProviderResponseAndFutureSteps() throws Exception {
        var started=new CountDownLatch(1);var release=new CountDownLatch(1);
        when(planner.generateStructured(any(),anyMap())).thenAnswer(i->{started.countDown();release.await(3,TimeUnit.SECONDS);return decision("SEARCH_NEWS","AI");});
        var t=create();var pending=CompletableFuture.supplyAsync(()->agent.advance(user,t.id()));
        assertTrue(started.await(2,TimeUnit.SECONDS));agent.cancel(user,t.id());release.countDown();
        assertEquals("CANCELLED",pending.get(3,TimeUnit.SECONDS).status());verify(tools,never()).execute(any(),any(),any(),any());
    }
    @Test void maliciousToolNameNeverExecutesAndOwnershipCannotBeForged(){
        when(planner.generateStructured(any(),anyMap())).thenReturn(decision("DELETE_ACCOUNT",""));
        var t=create();assertEquals("FAILED",agent.advance(user,t.id()).status());
        verify(tools,never()).execute(any(),any(),any(),any());
        assertThrows(AssistantException.class,()->agent.get(UUID.randomUUID(),t.id()));
        assertThrows(AssistantException.class,()->agent.cancel(UUID.randomUUID(),t.id()));
    }
    @Test void limitsRemainCumulativeAcrossRetries(){
        limits.setMaxSteps(2);when(planner.generateStructured(any(),anyMap())).thenThrow(new IllegalStateException());
        var t=create();agent.advance(user,t.id());agent.resume(user,t.id(),session);agent.advance(user,t.id());
        assertThrows(AssistantException.class,()->agent.resume(user,t.id(),session));
    }
    @Test void disabledActionsCannotCreatePendingConfirmation(){
        when(planner.generateStructured(any(),anyMap())).thenReturn(decision("SAVE_POST",UUID.randomUUID().toString()));
        var t=create();assertEquals("FAILED",agent.advance(user,t.id()).status());verifyNoInteractions(confirmations);
    }
    @Test void projectSwitchRequiresReturningToOriginalScope(){
        var t=create();agent.cancel(user,t.id());
        when(preferences.get(user)).thenReturn(new AssistantPreferencesService.Settings("TUTOR",true,false,false,"Other",false));
        assertThrows(AssistantException.class,()->agent.resume(user,t.id(),session));
    }
    @Test void refreshedResourceContentReplacesStaleCheckpointBeforePlanning(){
        var resource=UUID.randomUUID().toString();
        when(planner.generateStructured(any(),anyMap())).thenReturn(decision("GET_CONVERSATION",resource),done());
        when(tools.execute(user,"GET_CONVERSATION",Map.of("id",resource),null))
            .thenReturn(new AssistantToolRegistry.Result("GET_CONVERSATION","notice","Conversation","Old content",List.of(),null,null,"neutral"))
            .thenReturn(new AssistantToolRegistry.Result("GET_CONVERSATION","notice","Conversation","Current content",List.of(),null,null,"neutral"));
        var t=create();agent.advance(user,t.id());var result=agent.advance(user,t.id());
        assertEquals("COMPLETED",result.status());assertEquals("Current content",result.state().steps().getFirst().result().text());
        var requests=org.mockito.ArgumentCaptor.forClass(com.abhiai.abhiai_backend.ai.AiChatRequest.class);
        verify(planner,times(2)).generateStructured(requests.capture(),anyMap());
        String prompt=requests.getAllValues().get(1).messages().toString();
        assertTrue(prompt.contains("Current content"));assertFalse(prompt.contains("Old content"));
    }
    @Test void contextBudgetIncludesInstructionsAndToolDefinitions(){
        limits.setMaxContextSize(500);
        var t=create();assertEquals("FAILED",agent.advance(user,t.id()).status());
        verifyNoInteractions(planner);
    }
}
