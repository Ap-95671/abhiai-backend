package com.abhiai.abhiai_backend.dto.creator;
import java.time.LocalDate;
public record CreatorDailyMetricResponse(LocalDate date,long impressions,long uniquePostViewers,long profileViews,long uniqueProfileViewers,long engagements,long followerGrowth){}
