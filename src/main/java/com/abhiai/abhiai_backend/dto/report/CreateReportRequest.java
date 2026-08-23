package com.abhiai.abhiai_backend.dto.report;
import java.util.UUID; import com.abhiai.abhiai_backend.entity.*; import jakarta.validation.constraints.*;
public record CreateReportRequest(@NotNull ReportTargetType targetType,ReportTargetContext targetContext,@NotNull UUID targetId,@NotNull ReportReason reason,@Size(max=1000) String details){}
