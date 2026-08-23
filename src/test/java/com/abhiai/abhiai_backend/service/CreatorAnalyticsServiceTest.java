package com.abhiai.abhiai_backend.service;
import static org.junit.jupiter.api.Assertions.*; import static org.mockito.ArgumentMatchers.*; import static org.mockito.Mockito.*;
import java.time.LocalDate; import java.util.*;
import org.junit.jupiter.api.BeforeEach; import org.junit.jupiter.api.Test; import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock; import org.mockito.junit.jupiter.MockitoExtension; import org.springframework.test.util.ReflectionTestUtils;
import com.abhiai.abhiai_backend.entity.*; import com.abhiai.abhiai_backend.repository.*;
@ExtendWith(MockitoExtension.class)
class CreatorAnalyticsServiceTest {
 @Mock CreatorDailyMetricRepository metrics; @Mock UserRepository users; @Mock PostAccessService postAccess; @Mock UsernamePolicy usernames;
 private CreatorAnalyticsService service; private User creator,viewer; private Post post; private final UUID creatorId=UUID.randomUUID(),viewerId=UUID.randomUUID(),postId=UUID.randomUUID();
 @BeforeEach void setup(){service=new CreatorAnalyticsService(metrics,users,postAccess,usernames);creator=user(creatorId,"creator");viewer=user(viewerId,"viewer");post=new Post(creator,"Insight",PostVisibility.PUBLIC);ReflectionTestUtils.setField(post,"id",postId);}
 @Test void recordsImpressionAndDailyUniqueViewer(){when(postAccess.findViewablePost(viewerId,postId)).thenReturn(post);when(metrics.insertPostUnique(eq(postId),eq(viewerId),any())).thenReturn(1);var result=service.recordPostImpression(viewerId,postId);assertTrue(result.counted());verify(metrics).recordPostImpression(any(),eq(postId),any(),eq(1));verify(metrics).recordCreatorImpression(any(),eq(creatorId),any(),eq(1));}
 @Test void doesNotCountAuthorsOwnImpression(){when(postAccess.findViewablePost(creatorId,postId)).thenReturn(post);assertFalse(service.recordPostImpression(creatorId,postId).counted());verifyNoInteractions(metrics);}
 @Test void countsOneProfileViewPerViewerPerDay(){when(usernames.normalizeAndValidate("creator")).thenReturn("creator");when(users.findByUsernameIgnoreCase("creator")).thenReturn(Optional.of(creator));when(metrics.insertProfileUnique(eq(creatorId),eq(viewerId),any())).thenReturn(1);assertTrue(service.recordProfileView(viewerId,"creator").counted());verify(metrics).recordProfileView(any(),eq(creatorId),any(),eq(1));}
 @Test void dashboardAlwaysContainsEveryDayInRange(){when(users.findById(creatorId)).thenReturn(Optional.of(creator));when(metrics.findDaily(eq(creatorId),any(LocalDate.class))).thenReturn(List.of());when(metrics.findTopPosts(eq(creatorId),any(),any())).thenReturn(List.of());when(metrics.findAudienceLocations(eq(creatorId),any())).thenReturn(List.of());var result=service.dashboard(creatorId,7);assertEquals(7,result.daily().size());assertEquals(0,result.engagementRate());assertEquals(0,result.impressions());}
 private User user(UUID id,String username){User value=new User(username,username,username+"@example.com","hash");ReflectionTestUtils.setField(value,"id",id);return value;}
}
