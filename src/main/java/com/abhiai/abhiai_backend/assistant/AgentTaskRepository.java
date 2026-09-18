package com.abhiai.abhiai_backend.assistant;
import java.util.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
public interface AgentTaskRepository extends JpaRepository<AgentTask,UUID> {
    List<AgentTask> findByUserIdOrderByUpdatedAtDesc(UUID userId,Pageable page);
    List<AgentTask> findByUserIdAndStatusIn(UUID userId,Collection<String> statuses);
    Optional<AgentTask> findByIdAndUserId(UUID id,UUID userId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from AgentTask t where t.id=:id and t.userId=:userId")
    Optional<AgentTask> lock(UUID id,UUID userId);
}
