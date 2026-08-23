package com.abhiai.abhiai_backend.entity;
import java.time.LocalDate; import java.util.UUID; import jakarta.persistence.*;
@Entity @Table(name="post_daily_metrics",uniqueConstraints=@UniqueConstraint(name="uq_post_daily_metrics",columnNames={"post_id","metric_date"}))
public class PostDailyMetric {
 @Id private UUID id; @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="post_id",nullable=false) private Post post;
 @Column(name="metric_date",nullable=false) private LocalDate metricDate; @Column(nullable=false) private long impressions;
 @Column(name="unique_viewers",nullable=false) private long uniqueViewers; @Column(nullable=false) private long engagements;
 protected PostDailyMetric(){} public UUID getId(){return id;} public LocalDate getMetricDate(){return metricDate;}
}
