package com.abhiai.abhiai_backend.repository;
import java.util.*;import org.springframework.data.domain.*;import org.springframework.data.jpa.repository.*;import com.abhiai.abhiai_backend.entity.*;
public interface FollowRequestRepository extends JpaRepository<FollowRequest,UUID>{Optional<FollowRequest> findByRequesterIdAndTargetId(UUID r,UUID t);@EntityGraph(attributePaths="requester") Page<FollowRequest> findByTargetIdAndStatus(UUID t,FollowRequestStatus s,Pageable p);}
