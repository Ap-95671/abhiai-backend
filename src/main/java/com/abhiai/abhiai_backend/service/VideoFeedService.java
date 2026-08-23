package com.abhiai.abhiai_backend.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.abhiai.abhiai_backend.dto.common.PageResponse;
import com.abhiai.abhiai_backend.dto.post.PostResponse;
import com.abhiai.abhiai_backend.dto.post.PostViewResponse;
import com.abhiai.abhiai_backend.entity.Post;
import com.abhiai.abhiai_backend.exception.InvalidPostException;
import com.abhiai.abhiai_backend.exception.UserNotFoundException;
import com.abhiai.abhiai_backend.repository.PostRepository;
import com.abhiai.abhiai_backend.repository.PostViewRepository;
import com.abhiai.abhiai_backend.repository.UserRepository;

@Service
public class VideoFeedService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final Sort CHRONOLOGICAL_SORT = Sort.by(
            Sort.Order.desc("createdAt"),
            Sort.Order.desc("id"));

    private final PostRepository postRepository;
    private final PostViewRepository postViewRepository;
    private final PostAccessService postAccessService;
    private final UserRepository userRepository;

    public VideoFeedService(
            PostRepository postRepository,
            PostViewRepository postViewRepository,
            PostAccessService postAccessService,
            UserRepository userRepository) {
        this.postRepository = postRepository;
        this.postViewRepository = postViewRepository;
        this.postAccessService = postAccessService;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<PostResponse> getVideoFeed(UUID actingUserId, Pageable pageable) {
        requireUser(actingUserId);
        Page<Post> posts = postRepository.findVideoFeed(actingUserId, normalize(pageable));
        return PageResponse.from(posts, PostResponse::from);
    }

    @Transactional
    public PostViewResponse recordView(UUID actingUserId, UUID postId) {
        requireUser(actingUserId);
        Post post = postAccessService.findViewablePost(actingUserId, postId);
        boolean isVideo = post.getMedia().stream()
                .anyMatch(media -> media.getContentType().equals("video/mp4")
                        || media.getContentType().equals("video/webm"));
        if (!isVideo) {
            throw new InvalidPostException("Views can only be recorded for video posts");
        }
        if (actingUserId.equals(post.getAuthor().getId())) {
            return new PostViewResponse(postId, post.getViewCount(), false);
        }

        int inserted = postViewRepository.insertIfAbsent(
                UUID.randomUUID(), postId, actingUserId);
        if (inserted == 1) {
            postRepository.incrementViewCount(postId);
        }
        return new PostViewResponse(postId, post.getViewCount() + inserted, inserted == 1);
    }

    private void requireUser(UUID actingUserId) {
        if (!userRepository.existsById(actingUserId)) {
            throw new UserNotFoundException();
        }
    }

    private Pageable normalize(Pageable pageable) {
        int pageSize = Math.max(1, Math.min(pageable.getPageSize(), MAX_PAGE_SIZE));
        return PageRequest.of(pageable.getPageNumber(), pageSize, CHRONOLOGICAL_SORT);
    }
}
