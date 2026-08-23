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
import com.abhiai.abhiai_backend.dto.repost.PostRepostResponse;
import com.abhiai.abhiai_backend.dto.repost.PostRepostStatusResponse;
import com.abhiai.abhiai_backend.dto.repost.PostRepostUserResponse;
import com.abhiai.abhiai_backend.entity.Post;
import com.abhiai.abhiai_backend.entity.PostRepost;
import com.abhiai.abhiai_backend.entity.NotificationType;
import com.abhiai.abhiai_backend.entity.User;
import com.abhiai.abhiai_backend.exception.DuplicatePostRepostException;
import com.abhiai.abhiai_backend.exception.PostRepostNotFoundException;
import com.abhiai.abhiai_backend.exception.UserNotFoundException;
import com.abhiai.abhiai_backend.repository.PostRepository;
import com.abhiai.abhiai_backend.repository.PostRepostRepository;
import com.abhiai.abhiai_backend.repository.UserRepository;

@Service
public class PostRepostService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Sort REPOST_SORT = Sort.by(
            Sort.Order.desc("createdAt"),
            Sort.Order.desc("id"));

    private final PostRepostRepository postRepostRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PostAccessService postAccessService;
    private final SocialNotificationService notificationService;
    private final CreatorAnalyticsService analyticsService;

    public PostRepostService(
            PostRepostRepository postRepostRepository,
            PostRepository postRepository,
            UserRepository userRepository,
            PostAccessService postAccessService,
            SocialNotificationService notificationService,
            CreatorAnalyticsService analyticsService) {
        this.postRepostRepository = postRepostRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.postAccessService = postAccessService;
        this.notificationService = notificationService;
        this.analyticsService = analyticsService;
    }

    @Transactional
    public PostRepostResponse repost(UUID actingUserId, UUID postId) {
        Post post = postAccessService.findViewablePost(actingUserId, postId);
        User user = userRepository.findById(actingUserId).orElseThrow(UserNotFoundException::new);
        if (postRepostRepository.existsByPostIdAndUserId(postId, actingUserId)) {
            throw new DuplicatePostRepostException();
        }

        try {
            PostRepost repost = postRepostRepository.saveAndFlush(new PostRepost(post, user));
            notificationService.notifyPostInteraction(NotificationType.POST_REPOST, user, post);
            postRepository.incrementRepostCount(postId);
            analyticsService.recordEngagement(post, actingUserId);
            return PostRepostResponse.from(repost);
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicatePostRepostException();
        }
    }

    @Transactional
    public void removeRepost(UUID actingUserId, UUID postId) {
        postAccessService.findActivePost(postId);
        PostRepost repost = postRepostRepository.findByPostIdAndUserId(postId, actingUserId)
                .orElseThrow(PostRepostNotFoundException::new);
        postRepostRepository.delete(repost);
        postRepostRepository.flush();
        postRepository.decrementRepostCount(postId);
    }

    @Transactional(readOnly = true)
    public PostRepostStatusResponse getStatus(UUID actingUserId, UUID postId) {
        postAccessService.findViewablePost(actingUserId, postId);
        return new PostRepostStatusResponse(
                postId,
                postRepostRepository.existsByPostIdAndUserId(postId, actingUserId));
    }

    @Transactional(readOnly = true)
    public PageResponse<PostRepostUserResponse> getReposts(
            UUID actingUserId,
            UUID postId,
            Pageable pageable) {
        postAccessService.findViewablePost(actingUserId, postId);
        Page<PostRepost> reposts = postRepostRepository.findByPostId(postId, normalize(pageable));
        return PageResponse.from(reposts, PostRepostUserResponse::from);
    }

    private Pageable normalize(Pageable pageable) {
        int pageSize = Math.max(1, Math.min(pageable.getPageSize(), MAX_PAGE_SIZE));
        return PageRequest.of(pageable.getPageNumber(), pageSize, REPOST_SORT);
    }
}
