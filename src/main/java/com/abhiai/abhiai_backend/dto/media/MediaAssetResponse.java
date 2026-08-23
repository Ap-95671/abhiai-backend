package com.abhiai.abhiai_backend.dto.media;

import java.time.Instant;
import java.util.UUID;
import com.abhiai.abhiai_backend.entity.MediaAsset;
import com.abhiai.abhiai_backend.entity.MediaProcessingStatus;

public record MediaAssetResponse(UUID id, String originalFilename, String contentType, MediaKind kind, long byteSize, MediaProcessingStatus processingStatus, boolean thumbnailAvailable, Instant createdAt) {
    public static MediaAssetResponse from(MediaAsset asset) { return new MediaAssetResponse(asset.getId(), asset.getOriginalFilename(), asset.getContentType(), MediaKind.fromContentType(asset.getContentType()), asset.getByteSize(), asset.getProcessingStatus(), asset.getThumbnailStorageKey()!=null, asset.getCreatedAt()); }
}
