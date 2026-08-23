package com.abhiai.abhiai_backend.service;

import java.time.*; import java.util.*;
import org.springframework.data.domain.PageRequest; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
import com.abhiai.abhiai_backend.dto.creator.*; import com.abhiai.abhiai_backend.entity.*; import com.abhiai.abhiai_backend.exception.*;
import com.abhiai.abhiai_backend.repository.*;

@Service
public class CreatorAnalyticsService {
 private final CreatorDailyMetricRepository metrics; private final UserRepository users; private final PostAccessService postAccess; private final UsernamePolicy usernames;
 public CreatorAnalyticsService(CreatorDailyMetricRepository metrics,UserRepository users,PostAccessService postAccess,UsernamePolicy usernames){this.metrics=metrics;this.users=users;this.postAccess=postAccess;this.usernames=usernames;}

 @Transactional public AnalyticsRecordedResponse recordPostImpression(UUID viewerId,UUID postId){
  Post post=postAccess.findViewablePost(viewerId,postId); if(post.getAuthor().getId().equals(viewerId))return new AnalyticsRecordedResponse(postId,false);
  LocalDate today=LocalDate.now(ZoneOffset.UTC); int unique=metrics.insertPostUnique(postId,viewerId,today);
  metrics.recordPostImpression(UUID.randomUUID(),postId,today,unique); metrics.recordCreatorImpression(UUID.randomUUID(),post.getAuthor().getId(),today,unique);
  return new AnalyticsRecordedResponse(postId,true);
 }
 @Transactional public AnalyticsRecordedResponse recordProfileView(UUID viewerId,String requestedUsername){
  String username=usernames.normalizeAndValidate(requestedUsername); User profile=users.findByUsernameIgnoreCase(username).orElseThrow(UserNotFoundException::new);
  if(profile.getId().equals(viewerId))return new AnalyticsRecordedResponse(profile.getId(),false);
  LocalDate today=LocalDate.now(ZoneOffset.UTC); int unique=metrics.insertProfileUnique(profile.getId(),viewerId,today);
  if(unique==1)metrics.recordProfileView(UUID.randomUUID(),profile.getId(),today,1);
  return new AnalyticsRecordedResponse(profile.getId(),unique==1);
 }
 @Transactional public void recordEngagement(Post post,UUID actorId){if(post.getAuthor().getId().equals(actorId))return;LocalDate today=LocalDate.now(ZoneOffset.UTC);metrics.recordPostEngagement(UUID.randomUUID(),post.getId(),today);metrics.recordCreatorEngagement(UUID.randomUUID(),post.getAuthor().getId(),today);}
 @Transactional public void recordFollowerChange(UUID creatorId,boolean followed){LocalDate today=LocalDate.now(ZoneOffset.UTC);metrics.recordFollowerChange(UUID.randomUUID(),creatorId,today,followed?1:0,followed?0:1);}

 @Transactional(readOnly=true) public CreatorAnalyticsResponse dashboard(UUID creatorId,int requestedDays){
  User creator=users.findById(creatorId).orElseThrow(UserNotFoundException::new); int days=switch(requestedDays){case 7,30,90->requestedDays;default->30;};LocalDate to=LocalDate.now(ZoneOffset.UTC),from=to.minusDays(days-1L);
  Map<LocalDate,CreatorDailyMetricRepository.DailyProjection> stored=new HashMap<>();metrics.findDaily(creatorId,from).forEach(row->stored.put(row.getMetricDate(),row));
  List<CreatorDailyMetricResponse> daily=new ArrayList<>();long impressions=0,uniquePost=0,profileViews=0,uniqueProfile=0,engagements=0,growth=0;
  for(int offset=0;offset<days;offset++){LocalDate date=from.plusDays(offset);var row=stored.get(date);long i=row==null?0:row.getPostImpressions(),up=row==null?0:row.getUniquePostViewers(),pv=row==null?0:row.getProfileViews(),uv=row==null?0:row.getUniqueProfileViewers(),e=row==null?0:row.getEngagements(),g=row==null?0:row.getNewFollowers()-row.getUnfollows();daily.add(new CreatorDailyMetricResponse(date,i,up,pv,uv,e,g));impressions+=i;uniquePost+=up;profileViews+=pv;uniqueProfile+=uv;engagements+=e;growth+=g;}
  List<CreatorTopPostResponse> top=metrics.findTopPosts(creatorId,from,PageRequest.of(0,5)).stream().map(row->new CreatorTopPostResponse(row.getPostId(),row.getTextContent(),row.getImpressions(),row.getUniqueViewers(),row.getEngagements(),rate(row.getEngagements(),row.getImpressions()))).toList();
  long followers=creator.getFollowerCount();List<AudienceLocationResponse> locations=metrics.findAudienceLocations(creatorId,PageRequest.of(0,5)).stream().map(row->new AudienceLocationResponse(row.getLocation(),row.getAudienceCount(),followers==0?0:Math.round(row.getAudienceCount()*1000.0/followers)/10.0)).toList();
  return new CreatorAnalyticsResponse(days,from,to,impressions,uniquePost,profileViews,uniqueProfile,engagements,rate(engagements,impressions),growth,followers,List.copyOf(daily),top,locations);
 }
 private double rate(long engagements,long impressions){return impressions==0?0:Math.round(engagements*10000.0/impressions)/100.0;}
}
