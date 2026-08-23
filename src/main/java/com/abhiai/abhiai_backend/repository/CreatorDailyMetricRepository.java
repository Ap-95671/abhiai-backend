package com.abhiai.abhiai_backend.repository;

import java.time.LocalDate; import java.util.List; import java.util.UUID;
import org.springframework.data.domain.Pageable; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param;
import com.abhiai.abhiai_backend.entity.CreatorDailyMetric;

public interface CreatorDailyMetricRepository extends JpaRepository<CreatorDailyMetric,UUID>{
 interface DailyProjection { LocalDate getMetricDate(); long getPostImpressions(); long getUniquePostViewers(); long getProfileViews(); long getUniqueProfileViewers(); long getEngagements(); long getNewFollowers(); long getUnfollows(); }
 interface TopPostProjection { UUID getPostId(); String getTextContent(); long getImpressions(); long getUniqueViewers(); long getEngagements(); }
 interface LocationProjection { String getLocation(); long getAudienceCount(); }

 @Modifying @Query(value="insert into post_impression_uniques(post_id,viewer_id,metric_date) values (:postId,:viewerId,:date) on conflict do nothing",nativeQuery=true)
 int insertPostUnique(@Param("postId")UUID postId,@Param("viewerId")UUID viewerId,@Param("date")LocalDate date);
 @Modifying @Query(value="insert into profile_view_uniques(profile_id,viewer_id,metric_date) values (:profileId,:viewerId,:date) on conflict do nothing",nativeQuery=true)
 int insertProfileUnique(@Param("profileId")UUID profileId,@Param("viewerId")UUID viewerId,@Param("date")LocalDate date);
 @Modifying @Query(value="""
  insert into creator_daily_metrics(id,creator_id,metric_date,post_impressions,unique_post_viewers)
  values (:id,:creatorId,:date,1,:uniqueViewer) on conflict (creator_id,metric_date) do update
  set post_impressions=creator_daily_metrics.post_impressions+1,
      unique_post_viewers=creator_daily_metrics.unique_post_viewers+:uniqueViewer
  """,nativeQuery=true)
 int recordCreatorImpression(@Param("id")UUID id,@Param("creatorId")UUID creatorId,@Param("date")LocalDate date,@Param("uniqueViewer")int uniqueViewer);
 @Modifying @Query(value="""
  insert into post_daily_metrics(id,post_id,metric_date,impressions,unique_viewers)
  values (:id,:postId,:date,1,:uniqueViewer) on conflict (post_id,metric_date) do update
  set impressions=post_daily_metrics.impressions+1, unique_viewers=post_daily_metrics.unique_viewers+:uniqueViewer
  """,nativeQuery=true)
 int recordPostImpression(@Param("id")UUID id,@Param("postId")UUID postId,@Param("date")LocalDate date,@Param("uniqueViewer")int uniqueViewer);
 @Modifying @Query(value="""
  insert into creator_daily_metrics(id,creator_id,metric_date,profile_views,unique_profile_viewers)
  values (:id,:creatorId,:date,1,:uniqueViewer) on conflict (creator_id,metric_date) do update
  set profile_views=creator_daily_metrics.profile_views+1,
      unique_profile_viewers=creator_daily_metrics.unique_profile_viewers+:uniqueViewer
  """,nativeQuery=true)
 int recordProfileView(@Param("id")UUID id,@Param("creatorId")UUID creatorId,@Param("date")LocalDate date,@Param("uniqueViewer")int uniqueViewer);
 @Modifying @Query(value="""
  insert into creator_daily_metrics(id,creator_id,metric_date,engagements) values (:id,:creatorId,:date,1)
  on conflict (creator_id,metric_date) do update set engagements=creator_daily_metrics.engagements+1
  """,nativeQuery=true)
 int recordCreatorEngagement(@Param("id")UUID id,@Param("creatorId")UUID creatorId,@Param("date")LocalDate date);
 @Modifying @Query(value="""
  insert into post_daily_metrics(id,post_id,metric_date,engagements) values (:id,:postId,:date,1)
  on conflict (post_id,metric_date) do update set engagements=post_daily_metrics.engagements+1
  """,nativeQuery=true)
 int recordPostEngagement(@Param("id")UUID id,@Param("postId")UUID postId,@Param("date")LocalDate date);
 @Modifying @Query(value="""
  insert into creator_daily_metrics(id,creator_id,metric_date,new_followers,unfollows)
  values (:id,:creatorId,:date,:newFollowers,:unfollows) on conflict (creator_id,metric_date) do update
  set new_followers=creator_daily_metrics.new_followers+:newFollowers, unfollows=creator_daily_metrics.unfollows+:unfollows
  """,nativeQuery=true)
 int recordFollowerChange(@Param("id")UUID id,@Param("creatorId")UUID creatorId,@Param("date")LocalDate date,@Param("newFollowers")int newFollowers,@Param("unfollows")int unfollows);

 @Query(value="""
  select metric_date as metricDate,post_impressions as postImpressions,unique_post_viewers as uniquePostViewers,
  profile_views as profileViews,unique_profile_viewers as uniqueProfileViewers,engagements,
  new_followers as newFollowers,unfollows from creator_daily_metrics
  where creator_id=:creatorId and metric_date>=:from order by metric_date
  """,nativeQuery=true) List<DailyProjection> findDaily(@Param("creatorId")UUID creatorId,@Param("from")LocalDate from);
 @Query(value="""
  select p.id as postId,p.text_content as textContent,coalesce(sum(m.impressions),0) as impressions,
  coalesce(sum(m.unique_viewers),0) as uniqueViewers,coalesce(sum(m.engagements),0) as engagements
  from posts p left join post_daily_metrics m on m.post_id=p.id and m.metric_date>=:from
  where p.author_id=:creatorId and p.deleted_at is null group by p.id,p.text_content
  order by impressions desc,engagements desc,p.created_at desc
  """,nativeQuery=true) List<TopPostProjection> findTopPosts(@Param("creatorId")UUID creatorId,@Param("from")LocalDate from,Pageable pageable);
 @Query(value="""
  select coalesce(nullif(btrim(u.location),''),'Not specified') as location,count(*) as audienceCount
  from user_follows f join users u on u.id=f.follower_id where f.following_id=:creatorId
  group by coalesce(nullif(btrim(u.location),''),'Not specified') order by audienceCount desc
  """,nativeQuery=true) List<LocationProjection> findAudienceLocations(@Param("creatorId")UUID creatorId,Pageable pageable);
}
