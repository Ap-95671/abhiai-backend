package com.abhiai.abhiai_backend.repository;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.abhiai.abhiai_backend.entity.MediaAsset;
import com.abhiai.abhiai_backend.entity.MediaProcessingStatus;

public interface MediaAssetRepository extends JpaRepository<MediaAsset, UUID> {
    List<MediaAsset> findAllByIdInAndOwnerIdAndPostIsNull(Collection<UUID> ids, UUID ownerId);
    Optional<MediaAsset> findByIdAndOwnerIdAndPostIsNull(UUID id, UUID ownerId);
    List<MediaAsset> findAllByProcessingStatus(MediaProcessingStatus status);
    @Query(value = "select exists(select 1 from conversation_attachments where media_asset_id = :mediaId)", nativeQuery = true)
    boolean isUsedByConversation(@Param("mediaId") UUID mediaId);
}
