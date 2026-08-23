package com.abhiai.abhiai_backend.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.abhiai.abhiai_backend.entity.StoryReaction;

public interface StoryReactionRepository extends JpaRepository<StoryReaction, UUID> {
    Optional<StoryReaction> findByStoryIdAndUserId(UUID storyId, UUID userId);
    List<StoryReaction> findAllByUserIdAndStoryIdIn(UUID userId, Collection<UUID> storyIds);
}
