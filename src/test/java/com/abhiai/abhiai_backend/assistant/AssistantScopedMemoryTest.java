package com.abhiai.abhiai_backend.assistant;
import java.util.*;
import org.junit.jupiter.api.Test;
import com.abhiai.abhiai_backend.service.AiMemoryService;
import com.abhiai.abhiai_backend.repository.*;
import com.abhiai.abhiai_backend.entity.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class AssistantScopedMemoryTest {
    @Test void projectConversationAndSessionScopesAreIsolatedAndDisabledMeansNoRetrieval(){
        UUID id=UUID.randomUUID(),conversation=UUID.randomUUID(),session=UUID.randomUUID();
        var users=mock(UserRepository.class);var memories=mock(UserMemoryRepository.class);
        var user=new User("test","Test","t@example.com","hash");user.changeAiMemoryEnabled(true);when(users.findById(id)).thenReturn(Optional.of(user));
        var global=new UserMemory(user,"Be concise");var a=new UserMemory(user,"Project A preference");a.scope(MemoryScope.PROJECT,"A","");
        var b=new UserMemory(user,"Project B preference");b.scope(MemoryScope.PROJECT,"B","");
        var c=new UserMemory(user,"Conversation preference");c.scope(MemoryScope.CONVERSATION,conversation.toString(),"");
        var s=new UserMemory(user,"Session preference");s.scope(MemoryScope.SESSION,session.toString(),"");
        when(memories.findAllByUserIdOrderByUpdatedAtDesc(id)).thenReturn(List.of(global,a,b,c,s));var service=new AiMemoryService(users,memories);
        var selected=service.relevant(id,"preference","A",conversation,session);
        assertEquals(Set.of("Be concise","Project A preference","Conversation preference","Session preference"),new HashSet<>(selected.stream().map(m->m.content()).toList()));
        assertEquals(1,service.relevant(id,"preference").size());
        user.changeAiMemoryEnabled(false);assertTrue(service.relevant(id,"preference","A",conversation,session).isEmpty());
    }
    @Test void sameExplicitPreferenceSlotReplacesOlderValueInSameScopeOnly(){
        UUID id=UUID.randomUUID();var users=mock(UserRepository.class);var memories=mock(UserMemoryRepository.class);
        var user=new User("test","Test","t@example.com","hash");when(users.lockAssistantOwner(id)).thenReturn(Optional.of(user));
        var old=new UserMemory(user,"Give detailed answers");old.scope(MemoryScope.PROJECT,"A","response length");
        when(memories.findAllByUserIdOrderByUpdatedAtDesc(id)).thenReturn(List.of(old));when(memories.saveAndFlush(any())).thenAnswer(i->i.getArgument(0));
        var service=new AiMemoryService(users,memories);
        service.createScoped(id,"Keep answers short",MemoryCategory.PREFERENCE,MemoryScope.PROJECT,"A","response length");
        assertEquals("Keep answers short",old.getContent());verify(memories).saveAndFlush(old);
    }
    @Test void modesChangePresentationOnly(){
        assertTrue(AssistantPreferencesService.style("TUTOR").contains("step by step"));
        assertNotEquals(AssistantPreferencesService.style("STANDARD"),AssistantPreferencesService.style("TUTOR"));
        assertTrue(AssistantPersonality.INSTRUCTIONS.contains("UNTRUSTED DATA"));
    }
}
