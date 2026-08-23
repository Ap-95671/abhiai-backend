package com.abhiai.abhiai_backend.dto.creator;
import java.time.LocalDate; import java.util.List;
public record CreatorAnalyticsResponse(int days,LocalDate from,LocalDate to,long impressions,long uniquePostViewers,long profileViews,long uniqueProfileViewers,long engagements,double engagementRate,long followerGrowth,long totalFollowers,List<CreatorDailyMetricResponse> daily,List<CreatorTopPostResponse> topPosts,List<AudienceLocationResponse> audienceLocations){}
