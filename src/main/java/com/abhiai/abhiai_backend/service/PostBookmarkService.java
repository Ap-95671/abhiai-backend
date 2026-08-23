package com.abhiai.abhiai_backend.service;

import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.abhiai.abhiai_backend.dto.bookmark.BookmarkedPostResponse;
import com.abhiai.abhiai_backend.dto.bookmark.PostBookmarkResponse;
import com.abhiai.abhiai_backend.dto.bookmark.PostBookmarkStatusResponse;
import com.abhiai.abhiai_backend.dto.common.PageResponse;
import com.abhiai.abhiai_backend.entity.Post;
import com.abhiai.abhiai_backend.entity.PostBookmark;
import com.abhiai.abhiai_backend.entity.PostVisibility;
import com.abhiai.abhiai_backend.entity.User;
import com.abhiai.abhiai_backend.exception.DuplicatePostBookmarkException;
import com.abhiai.abhiai_backend.exception.PostBookmarkNotFoundException;
import com.abhiai.abhiai_backend.exception.UserNotFoundException;
import com.abhiai.abhiai_backend.repository.PostBookmarkRepository;
import com.abhiai.abhiai_backend.repository.PostRepository;
import com.abhiai.abhiai_backend.repository.UserRepository;

@Service
public class PostBookmarkService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Sort BOOKMARK_SORT = Sort.by(
            Sort.Order.desc("createdAt"),
            Sort.Order.desc("id"));

    private final PostBookmarkRepository postBookmarkRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PostAccessService postAccessService;

    public PostBookmarkService(
            PostBookmarkRepository postBookmarkRepository,
            PostRepository postRepository,
            UserRepository userRepository,
            PostAccessService postAccessService) {
        this.postBookmarkRepository = postBookmarkRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.postAccessService = postAccessService;
    }

    @Transactional
    public PostBookmarkResponse bookmark(UUID actingUserId, UUID postId) {
        Post post = postAccessService.findViewablePost(actingUserId, postId);
        User user = userRepository.findById(actingUserId).orElseThrow(UserNotFoundException::new);
        if (postBookmarkRepository.existsByPostIdAndUserId(postId, actingUserId)) {
            throw new DuplicatePostBookmarkException();
        }

        try {
            PostBookmark bookmark = postBookmarkRepository.saveAndFlush(new PostBookmark(post, user));
            postRepository.incrementBookmarkCount(postId);
            return PostBookmarkResponse.from(bookmark);
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicatePostBookmarkException();
        }
    }

    @Transactional
    public void removeBookmark(UUID actingUserId, UUID postId) {
        postAccessService.findActivePost(postId);
        PostBookmark bookmark = postBookmarkRepository.findByPostIdAndUserId(postId, actingUserId)
                .orElseThrow(PostBookmarkNotFoundException::new);
        postBookmarkRepository.delete(bookmark);
        postBookmarkRepository.flush();
        postRepository.decrementBookmarkCount(postId);
    }

    @Transactional(readOnly = true)
    public PostBookmarkStatusResponse getStatus(UUID actingUserId, UUID postId) {
        postAccessService.findViewablePost(actingUserId, postId);
        return new PostBookmarkStatusResponse(
                postId,
                postBookmarkRepository.existsByPostIdAndUserId(postId, actingUserId));
    }

    @Transactional(readOnly = true)
    public PageResponse<BookmarkedPostResponse> getBookmarks(UUID actingUserId, Pageable pageable) {
        if (!userRepository.existsById(actingUserId)) {
            throw new UserNotFoundException();
        }
        Page<PostBookmark> bookmarks = postBookmarkRepository.findAccessibleByUserId(
                actingUserId,
                PostVisibility.PUBLIC,
                PostVisibility.FOLLOWERS,
                normalize(pageable));
        return PageResponse.from(bookmarks, BookmarkedPostResponse::from);
    }

    private Pageable normalize(Pageable pageable) {
        int pageSize = Math.max(1, Math.min(pageable.getPageSize(), MAX_PAGE_SIZE));
        return PageRequest.of(pageable.getPageNumber(), pageSize, BOOKMARK_SORT);
    }
}
