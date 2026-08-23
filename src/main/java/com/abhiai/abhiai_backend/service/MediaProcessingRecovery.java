package com.abhiai.abhiai_backend.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.abhiai.abhiai_backend.entity.MediaProcessingStatus;
import com.abhiai.abhiai_backend.repository.MediaAssetRepository;

@Component
public class MediaProcessingRecovery implements ApplicationRunner {
    private final MediaAssetRepository repository; private final ApplicationEventPublisher events;
    public MediaProcessingRecovery(MediaAssetRepository repository,ApplicationEventPublisher events){this.repository=repository;this.events=events;}
    @Override @Transactional public void run(ApplicationArguments arguments){repository.findAllByProcessingStatus(MediaProcessingStatus.PENDING).forEach(asset->events.publishEvent(new MediaUploadedEvent(asset.getId())));}
}
