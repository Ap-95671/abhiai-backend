package com.abhiai.abhiai_backend.assistant;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
public interface PendingAssistantActionRepository extends JpaRepository<PendingAssistantAction,UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from PendingAssistantAction a where a.id=:id and a.userId=:userId")
    Optional<PendingAssistantAction> lock(UUID id,UUID userId);
    List<PendingAssistantAction> findByTaskId(UUID taskId);
}
