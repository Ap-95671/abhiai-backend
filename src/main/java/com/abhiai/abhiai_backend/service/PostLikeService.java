package com.abhiai.abhiai_backend.service;

import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.abhiai.abhiai_backend.dto.common.PageResponse;
import com.abhiai.abhiai_backend.dto.like.PostLikeResponse;
import com.abhiai.abhiai_backend.dto.like.PostLikeStatusResponse;
import com.abhiai.abhiai_backend.dto.like.PostLikeUserResponse;
import com.abhiai.abhiai_backend.entity.Post;
import com.abhiai.abhiai_backend.entity.PostLike;
import com.abhiai.abhiai_backend.entity.NotificationType;
import com.abhiai.abhiai_backend.entity.User;
import com.abhiai.abhiai_backend.exception.DuplicatePostLikeException;
import com.abhiai.abhiai_backend.exception.PostLikeNotFoundException;
import com.abhiai.abhiai_backend.exception.UserNotFoundException;
import com.abhiai.abhiai_backend.repository.PostLikeRepository;
import com.abhiai.abhiai_backend.repository.PostRepository;
import com.abhiai.abhiai_backend.repository.UserRepository;

@Service
public class PostLikeService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Sort LIKE_SORT = Sort.by(
            Sort.Order.desc("createdAt"),
            Sort.Order.desc("id"));

    private final PostLikeRepository postLikeRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PostAccessService postAccessService;
    private final SocialNotificationService notificationService;
    private final CreatorAnalyticsService analyticsService;

    public PostLikeService(
            PostLikeRepository postLikeRepository,
            PostRepository postRepository,
            UserRepository userRepository,
            PostAccessService postAccessService,
            SocialNotificationService notificationService,
            CreatorAnalyticsService analyticsService) {
        this.postLikeRepository = postLikeRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.postAccessService = postAccessService;
        this.notificationService = notificationService;
        this.analyticsService = analyticsService;
    }

    @Transactional
    public PostLikeResponse like(UUID actingUserId, UUID postId) {
        Post post = postAccessService.findViewablePost(actingUserId, postId);
        User user = userRepository.findById(actingUserId).orElseThrow(UserNotFoundException::new);
        if (postLikeRepository.existsByPostIdAndUserId(postId, actingUserId)) {
            throw new DuplicatePostLikeException();
        }

        try {
            PostLike like = postLikeRepository.saveAndFlush(new PostLike(post, user));
            notificationService.notifyPostInteraction(NotificationType.POST_LIKE, user, post);
            postRepository.incrementLikeCount(postId);
            analyticsService.recordEngagement(post, actingUserId);
            return PostLikeResponse.from(like);
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicatePostLikeException();
        }
    }

    @Transactional
    public void unlike(UUID actingUserId, UUID postId) {
        postAccessService.findActivePost(postId);
        PostLike like = postLikeRepository.findByPostIdAndUserId(postId, actingUserId)
                .orElseThrow(PostLikeNotFoundException::new);
        postLikeRepository.delete(like);
        postLikeRepository.flush();
        postRepository.decrementLikeCount(postId);
    }

    @Transactional(readOnly = true)
    public PostLikeStatusResponse getStatus(UUID actingUserId, UUID postId) {
        postAccessService.findViewablePost(actingUserId, postId);
        return new PostLikeStatusResponse(
                postId,
                postLikeRepository.existsByPostIdAndUserId(postId, actingUserId));
    }

    @Transactional(readOnly = true)
    public PageResponse<PostLikeUserResponse> getLikes(
            UUID actingUserId,
            UUID postId,
            Pageable pageable) {
        postAccessService.findViewablePost(actingUserId, postId);
        Page<PostLike> likes = postLikeRepository.findByPostId(postId, normalize(pageable));
        return PageResponse.from(likes, PostLikeUserResponse::from);
    }

    private Pageable normalize(Pageable pageable) {
        int pageSize = Math.max(1, Math.min(pageable.getPageSize(), MAX_PAGE_SIZE));
        return PageRequest.of(pageable.getPageNumber(), pageSize, LIKE_SORT);
    }
}
