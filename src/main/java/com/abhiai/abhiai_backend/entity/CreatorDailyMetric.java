package com.abhiai.abhiai_backend.entity;
import java.time.LocalDate; import java.util.UUID; import jakarta.persistence.*;
@Entity @Table(name="creator_daily_metrics",uniqueConstraints=@UniqueConstraint(name="uq_creator_daily_metrics",columnNames={"creator_id","metric_date"}))
public class CreatorDailyMetric {
 @Id private UUID id; @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="creator_id",nullable=false) private User creator;
 @Column(name="metric_date",nullable=false) private LocalDate metricDate; @Column(name="post_impressions",nullable=false) private long postImpressions;
 @Column(name="unique_post_viewers",nullable=false) private long uniquePostViewers; @Column(name="profile_views",nullable=false) private long profileViews;
 @Column(name="unique_profile_viewers",nullable=false) private long uniqueProfileViewers; @Column(nullable=false) private long engagements;
 @Column(name="new_followers",nullable=false) private long newFollowers; @Column(nullable=false) private long unfollows;
 protected CreatorDailyMetric(){} public UUID getId(){return id;} public LocalDate getMetricDate(){return metricDate;}
}
