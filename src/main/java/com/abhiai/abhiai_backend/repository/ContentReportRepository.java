package com.abhiai.abhiai_backend.repository;
import java.util.UUID; import org.springframework.data.domain.*; import org.springframework.data.jpa.repository.JpaRepository;
import com.abhiai.abhiai_backend.entity.*;
public interface ContentReportRepository extends JpaRepository<ContentReport,UUID>{
 boolean existsByReporterIdAndTargetTypeAndTargetContextAndTargetId(UUID reporterId,ReportTargetType type,ReportTargetContext context,UUID targetId);
 Page<ContentReport> findByReporterId(UUID reporterId,Pageable pageable);
}
