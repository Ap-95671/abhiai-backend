package com.abhiai.abhiai_backend.entity;
import java.time.Instant; import java.util.UUID; import org.hibernate.annotations.*; import jakarta.persistence.*;
@Entity @Table(name="content_reports",uniqueConstraints=@UniqueConstraint(name="uq_content_report",columnNames={"reporter_id","target_type","target_context","target_id"}))
public class ContentReport {
 @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="reporter_id",nullable=false) private User reporter;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="reported_user_id") private User reportedUser;
 @Enumerated(EnumType.STRING) @Column(name="target_type",nullable=false,length=24) private ReportTargetType targetType;
 @Enumerated(EnumType.STRING) @Column(name="target_context",length=32) private ReportTargetContext targetContext;
 @Column(name="target_id",nullable=false) private UUID targetId;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=32) private ReportReason reason;
 @Column(length=1000) private String details;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=16) private ReportStatus status=ReportStatus.PENDING;
 @CreationTimestamp @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
 @UpdateTimestamp @Column(name="updated_at",nullable=false) private Instant updatedAt; @Column(name="reviewed_at") private Instant reviewedAt;
 protected ContentReport(){} public ContentReport(User reporter,User reportedUser,ReportTargetType targetType,ReportTargetContext context,UUID targetId,ReportReason reason,String details){this.reporter=reporter;this.reportedUser=reportedUser;this.targetType=targetType;this.targetContext=context;this.targetId=targetId;this.reason=reason;this.details=details;}
 public UUID getId(){return id;} public User getReporter(){return reporter;} public User getReportedUser(){return reportedUser;} public ReportTargetType getTargetType(){return targetType;} public ReportTargetContext getTargetContext(){return targetContext;} public UUID getTargetId(){return targetId;} public ReportReason getReason(){return reason;} public String getDetails(){return details;} public ReportStatus getStatus(){return status;} public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;} public Instant getReviewedAt(){return reviewedAt;}
 public void review(ReportStatus next){status=next;reviewedAt=Instant.now();}
}
