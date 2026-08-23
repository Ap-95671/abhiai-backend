package com.abhiai.abhiai_backend.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.abhiai.abhiai_backend.config.PostProperties;
import com.abhiai.abhiai_backend.dto.common.PageResponse;
import com.abhiai.abhiai_backend.dto.reply.CreateReplyRequest;
import com.abhiai.abhiai_backend.dto.reply.PostReplyResponse;
import com.abhiai.abhiai_backend.entity.Post;
import com.abhiai.abhiai_backend.entity.PostReply;
import com.abhiai.abhiai_backend.entity.NotificationType;
import com.abhiai.abhiai_backend.entity.User;
import com.abhiai.abhiai_backend.exception.InvalidReplyException;
import com.abhiai.abhiai_backend.exception.PostReplyNotFoundException;
import com.abhiai.abhiai_backend.exception.UnauthorizedActionException;
import com.abhiai.abhiai_backend.exception.UserNotFoundException;
import com.abhiai.abhiai_backend.repository.PostReplyRepository;
import com.abhiai.abhiai_backend.repository.PostRepository;
import com.abhiai.abhiai_backend.repository.UserRepository;

@Service
public class PostReplyService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Sort REPLY_SORT = Sort.by(
            Sort.Order.desc("createdAt"),
            Sort.Order.desc("id"));

    private final PostReplyRepository postReplyRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PostAccessService postAccessService;
    private final PostProperties postProperties;
    private final SocialNotificationService notificationService;
    private final CreatorAnalyticsService analyticsService;

    public PostReplyService(
            PostReplyRepository postReplyRepository,
            PostRepository postRepository,
            UserRepository userRepository,
            PostAccessService postAccessService,
            PostProperties postProperties,
            SocialNotificationService notificationService,
            CreatorAnalyticsService analyticsService) {
        this.postReplyRepository = postReplyRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.postAccessService = postAccessService;
        this.postProperties = postProperties;
        this.notificationService = notificationService;
        this.analyticsService = analyticsService;
    }

    @Transactional
    public PostReplyResponse createReply(
            UUID actingUserId,
            UUID postId,
            CreateReplyRequest request) {
        String textContent = normalizeAndValidateText(request.textContent());
        Post post = postAccessService.findViewablePost(actingUserId, postId);
        User author = userRepository.findById(actingUserId).orElseThrow(UserNotFoundException::new);
        PostReply reply = postReplyRepository.saveAndFlush(new PostReply(post, author, textContent));
        notificationService.notifyPostInteraction(NotificationType.POST_REPLY, author, post);
        postRepository.incrementReplyCount(postId);
        analyticsService.recordEngagement(post, actingUserId);
        return PostReplyResponse.from(reply);
    }

    @Transactional(readOnly = true)
    public PageResponse<PostReplyResponse> getReplies(
            UUID actingUserId,
            UUID postId,
            Pageable pageable) {
        postAccessService.findViewablePost(actingUserId, postId);
        Page<PostReply> replies = postReplyRepository.findByPostIdAndDeletedAtIsNull(
                postId,
                normalize(pageable));
        return PageResponse.from(replies, PostReplyResponse::from);
    }

    @Transactional
    public void deleteReply(UUID actingUserId, UUID postId, UUID replyId) {
        postAccessService.findActivePost(postId);
        PostReply reply = postReplyRepository.findByIdAndPostIdAndDeletedAtIsNull(replyId, postId)
                .orElseThrow(PostReplyNotFoundException::new);
        if (!actingUserId.equals(reply.getAuthor().getId())) {
            throw new UnauthorizedActionException("Only the author can delete this reply");
        }

        reply.softDelete();
        postReplyRepository.saveAndFlush(reply);
        postRepository.decrementReplyCount(postId);
    }

    private String normalizeAndValidateText(String textContent) {
        String normalized = textContent == null ? "" : textContent.trim();
        if (normalized.isEmpty()) {
            throw new InvalidReplyException("Reply text is required");
        }
        int characterCount = normalized.codePointCount(0, normalized.length());
        if (characterCount > postProperties.getMaxTextLength()) {
            throw new InvalidReplyException(
                    "Reply text must not exceed " + postProperties.getMaxTextLength() + " characters");
        }
        return normalized;
    }

    private Pageable normalize(Pageable pageable) {
        int pageSize = Math.max(1, Math.min(pageable.getPageSize(), MAX_PAGE_SIZE));
        return PageRequest.of(pageable.getPageNumber(), pageSize, REPLY_SORT);
    }
}
