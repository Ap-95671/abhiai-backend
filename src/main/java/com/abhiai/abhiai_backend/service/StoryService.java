package com.abhiai.abhiai_backend.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.abhiai.abhiai_backend.config.StoryProperties;
import com.abhiai.abhiai_backend.dto.common.PageResponse;
import com.abhiai.abhiai_backend.dto.story.CreateStoryRequest;
import com.abhiai.abhiai_backend.dto.story.StoryReactionResponse;
import com.abhiai.abhiai_backend.dto.story.StoryResponse;
import com.abhiai.abhiai_backend.dto.story.StoryViewResponse;
import com.abhiai.abhiai_backend.entity.MediaAsset;
import com.abhiai.abhiai_backend.entity.Story;
import com.abhiai.abhiai_backend.entity.StoryReaction;
import com.abhiai.abhiai_backend.entity.StoryType;
import com.abhiai.abhiai_backend.entity.User;
import com.abhiai.abhiai_backend.exception.InvalidStoryException;
import com.abhiai.abhiai_backend.exception.StoryNotFoundException;
import com.abhiai.abhiai_backend.exception.UnauthorizedActionException;
import com.abhiai.abhiai_backend.exception.UserNotFoundException;
import com.abhiai.abhiai_backend.repository.MediaAssetRepository;
import com.abhiai.abhiai_backend.repository.StoryReactionRepository;
import com.abhiai.abhiai_backend.repository.StoryRepository;
import com.abhiai.abhiai_backend.repository.StoryViewRepository;
import com.abhiai.abhiai_backend.repository.UserRepository;

@Service
public class StoryService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final String DEFAULT_BACKGROUND = "#263B80";
    private static final Set<String> ALLOWED_REACTIONS = Set.of("❤️", "🔥", "😂", "👏", "😮");
    private static final Sort STORY_SORT = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final StoryRepository storyRepository;
    private final StoryViewRepository viewRepository;
    private final StoryReactionRepository reactionRepository;
    private final MediaAssetRepository mediaRepository;
    private final UserRepository userRepository;
    private final StoryProperties properties;

    public StoryService(
            StoryRepository storyRepository,
            StoryViewRepository viewRepository,
            StoryReactionRepository reactionRepository,
            MediaAssetRepository mediaRepository,
            UserRepository userRepository,
            StoryProperties properties) {
        this.storyRepository = storyRepository;
        this.viewRepository = viewRepository;
        this.reactionRepository = reactionRepository;
        this.mediaRepository = mediaRepository;
        this.userRepository = userRepository;
        this.properties = properties;
    }

    @Transactional
    public StoryResponse create(UUID actingUserId, CreateStoryRequest request) {
        User author = userRepository.findById(actingUserId).orElseThrow(UserNotFoundException::new);
        String text = normalize(request.textContent());
        MediaAsset media = request.mediaId() == null ? null : findAvailableMedia(actingUserId, request.mediaId());
        if (text == null && media == null) {
            throw new InvalidStoryException("A story needs text, an image, or a video");
        }
        StoryType type = determineType(media);
        Story story = storyRepository.saveAndFlush(new Story(
                author,
                media,
                type,
                text,
                request.backgroundColor() == null ? DEFAULT_BACKGROUND : request.backgroundColor().toUpperCase(),
                Instant.now().plus(properties.getLifetime())));
        return StoryResponse.from(story, true, null);
    }

    @Transactional(readOnly = true)
    public PageResponse<StoryResponse> getFeed(UUID actingUserId, Pageable pageable) {
        requireUser(actingUserId);
        Page<Story> page = storyRepository.findByExpiresAtAfter(Instant.now(), normalize(pageable));
        List<UUID> ids = page.getContent().stream().map(Story::getId).toList();
        if (ids.isEmpty()) {
            return PageResponse.from(page, story -> StoryResponse.from(story, false, null));
        }
        Set<UUID> viewed = Set.copyOf(viewRepository.findViewedStoryIds(actingUserId, ids));
        Map<UUID, String> reactions = reactionRepository.findAllByUserIdAndStoryIdIn(actingUserId, ids)
                .stream().collect(Collectors.toMap(reaction -> reaction.getStory().getId(), StoryReaction::getReaction));
        return PageResponse.from(page, story -> StoryResponse.from(
                story, viewed.contains(story.getId()), reactions.get(story.getId())));
    }

    @Transactional(readOnly = true)
    public StoryResponse get(UUID actingUserId, UUID storyId) {
        requireUser(actingUserId);
        Story story = activeStory(storyId);
        boolean viewed = viewRepository.findViewedStoryIds(actingUserId, List.of(storyId)).contains(storyId);
        String reaction = reactionRepository.findByStoryIdAndUserId(storyId, actingUserId)
                .map(StoryReaction::getReaction).orElse(null);
        return StoryResponse.from(story, viewed, reaction);
    }

    @Transactional
    public StoryViewResponse recordView(UUID actingUserId, UUID storyId) {
        requireUser(actingUserId);
        Story story = activeStory(storyId);
        if (actingUserId.equals(story.getAuthor().getId())) {
            return new StoryViewResponse(storyId, story.getViewCount(), false);
        }
        int inserted = viewRepository.insertIfAbsent(UUID.randomUUID(), storyId, actingUserId);
        if (inserted == 1) storyRepository.incrementViewCount(storyId);
        return new StoryViewResponse(storyId, story.getViewCount() + inserted, inserted == 1);
    }

    @Transactional
    public StoryReactionResponse react(UUID actingUserId, UUID storyId, String requestedReaction) {
        User user = userRepository.findById(actingUserId).orElseThrow(UserNotFoundException::new);
        Story story = activeStory(storyId);
        String reaction = requestedReaction.trim();
        if (!ALLOWED_REACTIONS.contains(reaction)) {
            throw new InvalidStoryException("Unsupported story reaction");
        }
        StoryReaction existing = reactionRepository.findByStoryIdAndUserId(storyId, actingUserId).orElse(null);
        long count = story.getReactionCount();
        if (existing == null) {
            reactionRepository.saveAndFlush(new StoryReaction(story, user, reaction));
            storyRepository.incrementReactionCount(storyId);
            count++;
        } else {
            existing.update(reaction);
            reactionRepository.saveAndFlush(existing);
        }
        return new StoryReactionResponse(storyId, reaction, count);
    }

    @Transactional
    public StoryReactionResponse removeReaction(UUID actingUserId, UUID storyId) {
        requireUser(actingUserId);
        Story story = activeStory(storyId);
        StoryReaction existing = reactionRepository.findByStoryIdAndUserId(storyId, actingUserId).orElse(null);
        long count = story.getReactionCount();
        if (existing != null) {
            reactionRepository.delete(existing);
            reactionRepository.flush();
            storyRepository.decrementReactionCount(storyId);
            count = Math.max(0, count - 1);
        }
        return new StoryReactionResponse(storyId, null, count);
    }

    @Transactional
    public void delete(UUID actingUserId, UUID storyId) {
        Story story = storyRepository.findById(storyId).orElseThrow(StoryNotFoundException::new);
        if (!actingUserId.equals(story.getAuthor().getId())) {
            throw new UnauthorizedActionException("Only the author can delete this story");
        }
        storyRepository.delete(story);
    }

    private MediaAsset findAvailableMedia(UUID ownerId, UUID mediaId) {
        MediaAsset media = mediaRepository.findByIdAndOwnerIdAndPostIsNull(mediaId, ownerId)
                .orElseThrow(() -> new InvalidStoryException("Story media must be uploaded by you and not attached to a post"));
        if (storyRepository.existsByMediaId(mediaId) || userRepository.isUsedAsProfileMedia(mediaId)) {
            throw new InvalidStoryException("This media is already in use");
        }
        if (!media.getContentType().startsWith("image/") && !media.getContentType().startsWith("video/")) {
            throw new InvalidStoryException("Stories support images and videos only");
        }
        return media;
    }

    private StoryType determineType(MediaAsset media) {
        if (media == null) return StoryType.TEXT;
        return media.getContentType().startsWith("video/") ? StoryType.VIDEO : StoryType.IMAGE;
    }

    private String normalize(String value) {
        if (value == null) return null;
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private Story activeStory(UUID storyId) {
        return storyRepository.findByIdAndExpiresAtAfter(storyId, Instant.now())
                .orElseThrow(StoryNotFoundException::new);
    }

    private void requireUser(UUID userId) {
        if (!userRepository.existsById(userId)) throw new UserNotFoundException();
    }

    private Pageable normalize(Pageable pageable) {
        int size = Math.max(1, Math.min(pageable.getPageSize(), MAX_PAGE_SIZE));
        return PageRequest.of(pageable.getPageNumber(), size, STORY_SORT);
    }
}
