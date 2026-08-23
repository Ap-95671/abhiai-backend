package com.abhiai.abhiai_backend.service;

import java.util.UUID;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.abhiai.abhiai_backend.config.PostProperties;
import com.abhiai.abhiai_backend.dto.post.CreatePostRequest;
import com.abhiai.abhiai_backend.dto.post.CreatePollRequest;
import com.abhiai.abhiai_backend.dto.post.PostResponse;
import com.abhiai.abhiai_backend.dto.post.UpdatePostRequest;
import com.abhiai.abhiai_backend.entity.Post;
import com.abhiai.abhiai_backend.entity.PostVisibility;
import com.abhiai.abhiai_backend.entity.Community;
import com.abhiai.abhiai_backend.entity.User;
import com.abhiai.abhiai_backend.exception.InvalidPostException;
import com.abhiai.abhiai_backend.exception.UnauthorizedActionException;
import com.abhiai.abhiai_backend.exception.UserNotFoundException;
import com.abhiai.abhiai_backend.repository.PostRepository;
import com.abhiai.abhiai_backend.repository.UserRepository;
import com.abhiai.abhiai_backend.repository.MediaAssetRepository;
import com.abhiai.abhiai_backend.repository.PostPollRepository;
import com.abhiai.abhiai_backend.entity.MediaAsset;
import com.abhiai.abhiai_backend.entity.PostPoll;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PostAccessService postAccessService;
    private final PostProperties postProperties;
    private final MediaAssetRepository mediaAssetRepository;
    private final HashtagService hashtagService;
    private final MentionService mentionService;
    private final PostPollRepository pollRepository;

    public PostService(
            PostRepository postRepository,
            UserRepository userRepository,
            PostAccessService postAccessService,
            PostProperties postProperties,
            MediaAssetRepository mediaAssetRepository,
            HashtagService hashtagService,
            MentionService mentionService,
            PostPollRepository pollRepository) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.postAccessService = postAccessService;
        this.postProperties = postProperties;
        this.mediaAssetRepository = mediaAssetRepository;
        this.hashtagService = hashtagService;
        this.mentionService = mentionService;
        this.pollRepository = pollRepository;
    }

    @Transactional
    public PostResponse createPost(UUID actingUserId, CreatePostRequest request) {
        return createPost(
                actingUserId,
                request.textContent(),
                request.visibility() == null ? PostVisibility.PUBLIC : request.visibility(),
                request.mediaIds(),
                null,
                request.poll());
    }

    PostResponse createCommunityPost(
            UUID actingUserId,
            Community community,
            String textContent,
            List<UUID> mediaIds) {
        return createPost(
                actingUserId,
                textContent,
                PostVisibility.PUBLIC,
                mediaIds,
                community,
                null);
    }

    private PostResponse createPost(
            UUID actingUserId,
            String requestedText,
            PostVisibility visibility,
            List<UUID> requestedMediaIds,
            Community community,
            CreatePollRequest pollRequest) {
        String textContent = normalizeAndValidateText(requestedText);
        User author = userRepository.findById(actingUserId).orElseThrow(UserNotFoundException::new);

        Post post = postRepository.saveAndFlush(new Post(author, textContent, visibility, community));
        List<UUID> mediaIds=requestedMediaIds==null?List.of():requestedMediaIds;
        if(mediaIds.stream().distinct().count()!=mediaIds.size()) throw new InvalidPostException("Duplicate media IDs are not allowed");
        List<MediaAsset> assets=mediaAssetRepository.findAllByIdInAndOwnerIdAndPostIsNull(mediaIds,actingUserId);
        if(assets.size()!=mediaIds.size()) throw new InvalidPostException("Every image must be uploaded by you and not already attached");
        assets.sort(java.util.Comparator.comparingInt(asset->mediaIds.indexOf(asset.getId()))); post.attachMedia(assets); mediaAssetRepository.saveAll(assets);
        if (pollRequest != null) {
            List<String> choices = normalizePollChoices(pollRequest.choices());
            PostPoll poll = new PostPoll(
                    post, Instant.now().plus(pollRequest.durationHours(), ChronoUnit.HOURS), choices);
            pollRepository.saveAndFlush(poll);
            post.attachPoll(poll);
        }
        userRepository.incrementPostCount(actingUserId);
        hashtagService.synchronize(post, textContent);
        mentionService.synchronize(post, textContent);
        return PostResponse.from(post);
    }

    @Transactional(readOnly = true)
    public PostResponse getPost(UUID actingUserId, UUID postId) {
        Post post = postAccessService.findViewablePost(actingUserId, postId);
        return PostResponse.from(post);
    }

    @Transactional
    public PostResponse updatePost(UUID actingUserId, UUID postId, UpdatePostRequest request) {
        Post post = postAccessService.findActivePost(postId);
        requireAuthor(actingUserId, post, "Only the author can update this post");

        if (request.textContent() == null && request.visibility() == null) {
            throw new InvalidPostException("At least one post field must be provided");
        }
        if (post.getCommunity() != null
                && request.visibility() != null
                && request.visibility() != PostVisibility.PUBLIC) {
            throw new InvalidPostException("Community posts must remain public");
        }

        String textContent = request.textContent() == null
                ? null
                : normalizeAndValidateText(request.textContent());
        post.update(textContent, request.visibility());
        Post saved = postRepository.saveAndFlush(post);
        hashtagService.synchronize(saved, saved.getTextContent());
        mentionService.synchronize(saved, saved.getTextContent());
        return PostResponse.from(saved);
    }

    @Transactional
    public void deletePost(UUID actingUserId, UUID postId) {
        Post post = postAccessService.findActivePost(postId);
        requireAuthor(actingUserId, post, "Only the author can delete this post");

        hashtagService.synchronize(post, "");
        mentionService.synchronize(post, "");
        post.softDelete();
        postRepository.saveAndFlush(post);
        userRepository.decrementPostCount(actingUserId);
    }

    @Transactional
    public PostResponse pinPost(UUID actingUserId, UUID postId) {
        Post post = postAccessService.findActivePost(postId);
        requireAuthor(actingUserId, post, "Only the author can pin this post");
        postRepository.clearPinnedPostByAuthorId(actingUserId);
        post.pin();
        return PostResponse.from(postRepository.saveAndFlush(post));
    }

    @Transactional
    public void unpinPost(UUID actingUserId, UUID postId) {
        Post post = postAccessService.findActivePost(postId);
        requireAuthor(actingUserId, post, "Only the author can unpin this post");
        post.unpin();
        postRepository.saveAndFlush(post);
    }

    private String normalizeAndValidateText(String textContent) {
        String normalized = textContent == null ? "" : textContent.trim();
        if (normalized.isEmpty()) {
            throw new InvalidPostException("Post text is required");
        }
        int characterCount = normalized.codePointCount(0, normalized.length());
        if (characterCount > postProperties.getMaxTextLength()) {
            throw new InvalidPostException(
                    "Post text must not exceed " + postProperties.getMaxTextLength() + " characters");
        }
        return normalized;
    }

    private List<String> normalizePollChoices(List<String> requestedChoices) {
        if (requestedChoices == null || requestedChoices.size() < 2 || requestedChoices.size() > 4) {
            throw new InvalidPostException("A poll must contain between 2 and 4 choices");
        }
        List<String> choices = requestedChoices.stream().map(value -> value == null ? "" : value.trim()).toList();
        if (choices.stream().anyMatch(String::isBlank)) throw new InvalidPostException("Poll choices must not be blank");
        Set<String> unique = choices.stream().map(value -> value.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
        if (unique.size() != choices.size()) throw new InvalidPostException("Poll choices must be unique");
        return choices;
    }

    private void requireAuthor(UUID actingUserId, Post post, String message) {
        if (!actingUserId.equals(post.getAuthor().getId())) {
            throw new UnauthorizedActionException(message);
        }
    }
}
