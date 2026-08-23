package com.abhiai.abhiai_backend.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.abhiai.abhiai_backend.entity.MediaAsset;
import com.abhiai.abhiai_backend.entity.Story;
import com.abhiai.abhiai_backend.repository.MediaAssetRepository;
import com.abhiai.abhiai_backend.repository.StoryRepository;

@Service
public class StoryCleanupService {

    private final StoryRepository storyRepository;
    private final MediaAssetRepository mediaRepository;
    private final MediaStorage mediaStorage;

    public StoryCleanupService(
            StoryRepository storyRepository,
            MediaAssetRepository mediaRepository,
            MediaStorage mediaStorage) {
        this.storyRepository = storyRepository;
        this.mediaRepository = mediaRepository;
        this.mediaStorage = mediaStorage;
    }

    @Scheduled(fixedDelayString = "${app.social.stories.cleanup-interval-ms:3600000}")
    @Transactional
    public void removeExpiredStories() {
        List<Story> expired = storyRepository.findTop100ByExpiresAtBeforeOrderByExpiresAtAsc(Instant.now());
        if (expired.isEmpty()) return;

        List<MediaAsset> media = expired.stream().map(Story::getMedia).filter(item -> item != null).toList();
        List<String> storageKeys = new ArrayList<>();
        media.forEach(asset -> {
            storageKeys.add(asset.getStorageKey());
            if (asset.getOptimizedStorageKey() != null) storageKeys.add(asset.getOptimizedStorageKey());
            if (asset.getThumbnailStorageKey() != null) storageKeys.add(asset.getThumbnailStorageKey());
        });

        storyRepository.deleteAll(expired);
        storyRepository.flush();
        mediaRepository.deleteAll(media);
        mediaRepository.flush();

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                storageKeys.forEach(mediaStorage::delete);
            }
        });
    }
}
