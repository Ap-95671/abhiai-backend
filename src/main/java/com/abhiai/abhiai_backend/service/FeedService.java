package com.abhiai.abhiai_backend.service;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.abhiai.abhiai_backend.dto.common.PageResponse;
import com.abhiai.abhiai_backend.dto.post.PostResponse;
import com.abhiai.abhiai_backend.entity.Post;
import com.abhiai.abhiai_backend.exception.UserNotFoundException;
import com.abhiai.abhiai_backend.repository.UserRepository;

@Service
public class FeedService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Sort CHRONOLOGICAL_SORT = Sort.by(
            Sort.Order.desc("createdAt"),
            Sort.Order.desc("id"));

    private final UserRepository userRepository;
    private final FeedStrategy feedStrategy;
    private final MuteService muteService;

    public FeedService(
            UserRepository userRepository,
            @Qualifier("chronologicalFeedStrategy") FeedStrategy feedStrategy) {
        this(userRepository, feedStrategy, null);
    }
    @org.springframework.beans.factory.annotation.Autowired
    public FeedService(UserRepository userRepository,
            @Qualifier("chronologicalFeedStrategy") FeedStrategy feedStrategy, MuteService muteService) {
        this.userRepository = userRepository;
        this.feedStrategy = feedStrategy;
        this.muteService = muteService;
    }

    @Transactional(readOnly = true)
    public PageResponse<PostResponse> getHomeFeed(UUID actingUserId, Pageable pageable) {
        if (!userRepository.existsById(actingUserId)) {
            throw new UserNotFoundException();
        }

        Page<Post> posts = feedStrategy.load(
                actingUserId,
                normalize(pageable));
        if (muteService == null) return PageResponse.from(posts, PostResponse::from);
        var visible = posts.getContent().stream().filter(post -> !muteService.muted(
                actingUserId, post.getAuthor().getId(), post.getTextContent())).map(PostResponse::from).toList();
        return new PageResponse<>(visible, posts.getNumber(), posts.getSize(), visible.size(),
                posts.getTotalPages(), posts.isFirst(), posts.isLast());
    }

    private Pageable normalize(Pageable pageable) {
        int pageSize = Math.max(1, Math.min(pageable.getPageSize(), MAX_PAGE_SIZE));
        return PageRequest.of(pageable.getPageNumber(), pageSize, CHRONOLOGICAL_SORT);
    }
}
