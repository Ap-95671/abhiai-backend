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
import com.abhiai.abhiai_backend.dto.follow.FollowActionResponse;
import com.abhiai.abhiai_backend.dto.follow.FollowStatusResponse;
import com.abhiai.abhiai_backend.dto.follow.FollowUserResponse;
import com.abhiai.abhiai_backend.entity.Follow;
import com.abhiai.abhiai_backend.entity.User;
import com.abhiai.abhiai_backend.exception.DuplicateFollowException;
import com.abhiai.abhiai_backend.exception.FollowRelationshipNotFoundException;
import com.abhiai.abhiai_backend.exception.InvalidFollowException;
import com.abhiai.abhiai_backend.exception.UserNotFoundException;
import com.abhiai.abhiai_backend.repository.FollowRepository;
import com.abhiai.abhiai_backend.repository.UserRepository;

@Service
public class FollowService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Sort FOLLOW_SORT = Sort.by(
            Sort.Order.desc("createdAt"),
            Sort.Order.desc("id"));

    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final SocialNotificationService notificationService;
    private final CreatorAnalyticsService analyticsService;
    private final BlockPolicyService blockPolicyService;
    private final FollowRequestService followRequestService;

    public FollowService(
            FollowRepository followRepository,
            UserRepository userRepository,
            SocialNotificationService notificationService,
            CreatorAnalyticsService analyticsService) {
        this(followRepository, userRepository, notificationService, analyticsService, null, null);
    }

    public FollowService(FollowRepository followRepository, UserRepository userRepository,
            SocialNotificationService notificationService, CreatorAnalyticsService analyticsService,
            BlockPolicyService blockPolicyService) {
        this(followRepository,userRepository,notificationService,analyticsService,blockPolicyService,null);
    }
    @org.springframework.beans.factory.annotation.Autowired
    public FollowService(FollowRepository followRepository, UserRepository userRepository,
            SocialNotificationService notificationService, CreatorAnalyticsService analyticsService,
            BlockPolicyService blockPolicyService, FollowRequestService followRequestService) {
        this.followRepository = followRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.analyticsService = analyticsService;
        this.blockPolicyService = blockPolicyService;
        this.followRequestService = followRequestService;
    }

    @Transactional
    public FollowActionResponse follow(UUID actingUserId, UUID targetUserId) {
        rejectSelfFollow(actingUserId, targetUserId, "You cannot follow yourself");
        if (blockPolicyService != null) blockPolicyService.requireInteractionAllowed(actingUserId, targetUserId);
        User actingUser = findUser(actingUserId);
        User targetUser = findUser(targetUserId);
        if (targetUser.getAccountPrivacy() == com.abhiai.abhiai_backend.entity.AccountPrivacy.PRIVATE
                && followRequestService != null) {
            followRequestService.request(actingUserId, targetUserId);
            return new FollowActionResponse(targetUserId, false, null);
        }

        if (followRepository.existsByFollowerIdAndFollowingId(actingUserId, targetUserId)) {
            throw new DuplicateFollowException();
        }

        try {
            Follow follow = followRepository.saveAndFlush(new Follow(actingUser, targetUser));
            notificationService.notifyFollow(actingUser, targetUser);
            userRepository.incrementFollowingCount(actingUserId);
            userRepository.incrementFollowerCount(targetUserId);
            analyticsService.recordFollowerChange(targetUserId, true);
            return FollowActionResponse.from(follow);
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateFollowException();
        }
    }

    @Transactional
    public void unfollow(UUID actingUserId, UUID targetUserId) {
        rejectSelfFollow(actingUserId, targetUserId, "You cannot unfollow yourself");
        findUser(targetUserId);

        Follow follow = followRepository.findByFollowerIdAndFollowingId(actingUserId, targetUserId)
                .orElseThrow(FollowRelationshipNotFoundException::new);

        followRepository.delete(follow);
        followRepository.flush();
        userRepository.decrementFollowingCount(actingUserId);
        userRepository.decrementFollowerCount(targetUserId);
        analyticsService.recordFollowerChange(targetUserId, false);
    }

    @Transactional(readOnly = true)
    public FollowStatusResponse getFollowStatus(UUID actingUserId, UUID targetUserId) {
        findUser(targetUserId);
        boolean following = !actingUserId.equals(targetUserId)
                && followRepository.existsByFollowerIdAndFollowingId(actingUserId, targetUserId);
        return new FollowStatusResponse(targetUserId, following);
    }

    @Transactional(readOnly = true)
    public PageResponse<FollowUserResponse> getFollowers(UUID userId, Pageable pageable) {
        findUser(userId);
        Page<Follow> follows = followRepository.findByFollowingId(userId, normalize(pageable));
        return PageResponse.from(
                follows,
                follow -> FollowUserResponse.from(follow.getFollower(), follow.getCreatedAt()));
    }

    @Transactional(readOnly = true)
    public PageResponse<FollowUserResponse> getFollowing(UUID userId, Pageable pageable) {
        findUser(userId);
        Page<Follow> follows = followRepository.findByFollowerId(userId, normalize(pageable));
        return PageResponse.from(
                follows,
                follow -> FollowUserResponse.from(follow.getFollowing(), follow.getCreatedAt()));
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
    }

    private Pageable normalize(Pageable pageable) {
        int pageSize = Math.max(1, Math.min(pageable.getPageSize(), MAX_PAGE_SIZE));
        return PageRequest.of(pageable.getPageNumber(), pageSize, FOLLOW_SORT);
    }

    private void rejectSelfFollow(UUID actingUserId, UUID targetUserId, String message) {
        if (actingUserId.equals(targetUserId)) {
            throw new InvalidFollowException(message);
        }
    }
}
