package com.abhiai.abhiai_backend.service;

import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.abhiai.abhiai_backend.dto.common.PageResponse;
import com.abhiai.abhiai_backend.dto.user.BlockStatusResponse;
import com.abhiai.abhiai_backend.dto.user.BlockedUserResponse;
import com.abhiai.abhiai_backend.entity.Follow;
import com.abhiai.abhiai_backend.entity.User;
import com.abhiai.abhiai_backend.entity.UserBlock;
import com.abhiai.abhiai_backend.exception.UnauthorizedActionException;
import com.abhiai.abhiai_backend.exception.UserNotFoundException;
import com.abhiai.abhiai_backend.repository.FollowRepository;
import com.abhiai.abhiai_backend.repository.UserBlockRepository;
import com.abhiai.abhiai_backend.repository.UserRepository;

@Service
public class UserBlockService {
    private final UserBlockRepository blockRepository;
    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final CreatorAnalyticsService analyticsService;

    public UserBlockService(UserBlockRepository blockRepository, FollowRepository followRepository,
            UserRepository userRepository, CreatorAnalyticsService analyticsService) {
        this.blockRepository = blockRepository;
        this.followRepository = followRepository;
        this.userRepository = userRepository;
        this.analyticsService = analyticsService;
    }

    @Transactional
    public BlockStatusResponse block(UUID actingUserId, UUID targetUserId) {
        if (actingUserId.equals(targetUserId)) throw new UnauthorizedActionException("You cannot block yourself");
        User actor = requireUser(actingUserId);
        User target = requireUser(targetUserId);
        if (!blockRepository.existsByBlockerIdAndBlockedId(actingUserId, targetUserId)) {
            try { blockRepository.saveAndFlush(new UserBlock(actor, target)); }
            catch (DataIntegrityViolationException ignored) { /* concurrent duplicate is idempotent */ }
            removeFollow(actingUserId, targetUserId);
            removeFollow(targetUserId, actingUserId);
        }
        return status(actingUserId, targetUserId);
    }

    @Transactional
    public BlockStatusResponse unblock(UUID actingUserId, UUID targetUserId) {
        requireUser(targetUserId);
        blockRepository.findByBlockerIdAndBlockedId(actingUserId, targetUserId).ifPresent(blockRepository::delete);
        blockRepository.flush();
        return status(actingUserId, targetUserId);
    }

    @Transactional(readOnly = true)
    public BlockStatusResponse status(UUID actingUserId, UUID targetUserId) {
        requireUser(targetUserId);
        return new BlockStatusResponse(targetUserId,
                blockRepository.existsByBlockerIdAndBlockedId(actingUserId, targetUserId),
                blockRepository.existsByBlockerIdAndBlockedId(targetUserId, actingUserId));
    }

    @Transactional(readOnly = true)
    public PageResponse<BlockedUserResponse> list(UUID actingUserId, Pageable pageable) {
        requireUser(actingUserId);
        int size = Math.max(1, Math.min(pageable.getPageSize(), 100));
        return PageResponse.from(blockRepository.findByBlockerId(actingUserId,
                PageRequest.of(Math.max(0, pageable.getPageNumber()), size)), BlockedUserResponse::from);
    }

    private void removeFollow(UUID followerId, UUID followingId) {
        Follow follow = followRepository.findByFollowerIdAndFollowingId(followerId, followingId).orElse(null);
        if (follow == null) return;
        followRepository.delete(follow);
        userRepository.decrementFollowingCount(followerId);
        userRepository.decrementFollowerCount(followingId);
        analyticsService.recordFollowerChange(followingId, false);
    }

    private User requireUser(UUID id) {
        return userRepository.findById(id).orElseThrow(UserNotFoundException::new);
    }
}
