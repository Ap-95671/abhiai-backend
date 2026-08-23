package com.abhiai.abhiai_backend.dto.report;
import java.time.Instant; import java.util.UUID; import com.abhiai.abhiai_backend.entity.*;
public record ContentReportResponse(UUID id,ReportTargetType targetType,ReportTargetContext targetContext,UUID targetId,ReportReason reason,String details,ReportStatus status,Instant createdAt){public static ContentReportResponse from(ContentReport report){return new ContentReportResponse(report.getId(),report.getTargetType(),report.getTargetContext(),report.getTargetId(),report.getReason(),report.getDetails(),report.getStatus(),report.getCreatedAt());}}
