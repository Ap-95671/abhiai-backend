package com.abhiai.abhiai_backend.assistant;
import java.util.*;
import java.time.Instant;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import com.abhiai.abhiai_backend.service.*;
import com.abhiai.abhiai_backend.entity.*;
import com.abhiai.abhiai_backend.dto.bookmark.PostBookmarkStatusResponse;
import tools.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class AssistantConfirmationTest {
    UUID user=UUID.randomUUID(),task=UUID.randomUUID(),postId=UUID.randomUUID();
    PendingAssistantActionRepository actions=mock(PendingAssistantActionRepository.class);
    AssistantPreferencesService preferences=mock(AssistantPreferencesService.class);
    PostAccessService access=mock(PostAccessService.class);
    PostBookmarkService bookmarks=mock(PostBookmarkService.class);
    Map<UUID,PendingAssistantAction> stored=new HashMap<>();Post post;
    AssistantConfirmationService service;
    @BeforeEach void setup(){
        when(preferences.get(user)).thenReturn(new AssistantPreferencesService.Settings("STANDARD",true,true,false,"",false));
        var owner=new User("tester","Test","test@example.com","hash");ReflectionTestUtils.setField(owner,"id",user);
        post=new Post(owner,"Reviewed post",PostVisibility.PUBLIC);ReflectionTestUtils.setField(post,"id",postId);
        when(access.findViewablePost(user,postId)).thenReturn(post);
        when(bookmarks.getStatus(user,postId)).thenReturn(new PostBookmarkStatusResponse(postId,false));
        when(actions.save(any())).thenAnswer(i->{PendingAssistantAction a=i.getArgument(0);stored.put(a.id,a);return a;});
        when(actions.lock(any(),any())).thenAnswer(i->Optional.ofNullable(stored.get(i.getArgument(0))).filter(a->a.userId.equals(i.getArgument(1))));
        service=new AssistantConfirmationService(actions,preferences,access,bookmarks,new ObjectMapper());
    }
    @Test void exactReviewedPayloadExecutesOnce(){
        var review=service.prepare(user,task,postId.toString());
        assertTrue(review.exactPayload().contains("Reviewed post"));
        assertThrows(AssistantException.class,()->service.approve(user,task,review.id(),"0".repeat(64)));
        verifyNoInteractions(bookmarks);
        service.approve(user,task,review.id(),review.payloadHash());verify(bookmarks).bookmark(user,postId);
        assertThrows(AssistantException.class,()->service.approve(user,task,review.id(),review.payloadHash()));verify(bookmarks,times(1)).bookmark(user,postId);
    }
    @Test void expiredModifiedOrForeignActionsCannotExecute(){
        var review=service.prepare(user,task,postId.toString());
        stored.get(review.id()).expiresAt=Instant.now().minusSeconds(1);
        assertThrows(AssistantException.class,()->service.approve(user,task,review.id(),review.payloadHash()));
        stored.get(review.id()).expiresAt=Instant.now().plusSeconds(60);post.update("Changed post",null);
        assertThrows(AssistantException.class,()->service.approve(user,task,review.id(),review.payloadHash()));
        assertThrows(AssistantException.class,()->service.approve(user,UUID.randomUUID(),review.id(),review.payloadHash()));
        verifyNoInteractions(bookmarks);
    }
    @Test void disablingActionsBetweenReviewAndConfirmationBlocksExecution(){
        var review=service.prepare(user,task,postId.toString());
        when(preferences.get(user)).thenReturn(new AssistantPreferencesService.Settings("STANDARD",true,false,false,"",false));
        assertThrows(AssistantException.class,()->service.approve(user,task,review.id(),review.payloadHash()));verifyNoInteractions(bookmarks);
    }
}
