package com.abhiai.abhiai_backend.service;

import java.text.Normalizer;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.abhiai.abhiai_backend.dto.common.PageResponse;
import com.abhiai.abhiai_backend.dto.hashtag.HashtagResponse;
import com.abhiai.abhiai_backend.dto.post.PostResponse;
import com.abhiai.abhiai_backend.dto.search.UserSearchResponse;
import com.abhiai.abhiai_backend.exception.InvalidSearchQueryException;
import com.abhiai.abhiai_backend.exception.UserNotFoundException;
import com.abhiai.abhiai_backend.repository.UserRepository;

@Service
public class SearchService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final int MIN_QUERY_LENGTH = 2;
    private static final int MAX_QUERY_LENGTH = 100;
    private static final Pattern USERNAME_PATTERN = Pattern.compile("[a-z0-9_]{3,30}");
    private static final Sort USER_SORT = Sort.by(
            Sort.Order.asc("username"), Sort.Order.asc("id"));
    private static final Sort HASHTAG_SORT = Sort.by(
            Sort.Order.desc("postCount"),
            Sort.Order.asc("normalizedTag"),
            Sort.Order.asc("id"));

    private final UserRepository userRepository;
    private final SearchProvider searchProvider;

    public SearchService(UserRepository userRepository, SearchProvider searchProvider) {
        this.userRepository = userRepository;
        this.searchProvider = searchProvider;
    }

    @Transactional(readOnly = true)
    public PageResponse<UserSearchResponse> searchUsers(String query, Pageable pageable) {
        String normalized = normalizeQuery(query);
        return PageResponse.from(
                searchProvider.searchUsers(normalized, normalize(pageable, USER_SORT)),
                UserSearchResponse::from);
    }

    @Transactional(readOnly = true)
    public PageResponse<PostResponse> searchPosts(
            UUID actingUserId,
            String query,
            PostSearchCriteria criteria,
            Pageable pageable) {
        requireUser(actingUserId);
        String normalized = normalizeQuery(query);
        PostSearchCriteria normalizedCriteria = normalizeCriteria(criteria);
        return PageResponse.from(
                searchProvider.searchPosts(
                        actingUserId, normalized, normalizedCriteria, normalize(pageable, Sort.unsorted())),
                PostResponse::from);
    }

    @Transactional(readOnly = true)
    public PageResponse<PostResponse> searchPosts(
            UUID actingUserId,
            String query,
            Pageable pageable) {
        return searchPosts(actingUserId, query, PostSearchCriteria.defaults(), pageable);
    }

    @Transactional(readOnly = true)
    public PageResponse<HashtagResponse> searchHashtags(String query, Pageable pageable) {
        String normalized = normalizeQuery(query);
        if (normalized.startsWith("#")) normalized = normalized.substring(1);
        String hashtagQuery = normalized.toLowerCase(Locale.ROOT);
        return PageResponse.from(
                searchProvider.searchHashtags(hashtagQuery, normalize(pageable, HASHTAG_SORT)),
                HashtagResponse::from);
    }

    private PostSearchCriteria normalizeCriteria(PostSearchCriteria criteria) {
        PostSearchCriteria value = criteria == null ? PostSearchCriteria.defaults() : criteria;
        if (value.fromDate() != null && value.toDate() != null
                && value.fromDate().isAfter(value.toDate())) {
            throw new InvalidSearchQueryException("Search start date must not be after the end date");
        }
        String author = value.authorUsername();
        if (author != null) {
            author = author.trim().toLowerCase(Locale.ROOT);
            if (author.startsWith("@")) author = author.substring(1);
            if (author.isEmpty()) {
                author = null;
            } else if (!USERNAME_PATTERN.matcher(author).matches()) {
                throw new InvalidSearchQueryException("Author must be a valid username");
            }
        }
        return new PostSearchCriteria(
                author,
                value.fromDate(),
                value.toDate(),
                value.hasMedia(),
                value.sort() == null ? SearchSort.RELEVANCE : value.sort());
    }

    private String normalizeQuery(String query) {
        String normalized = Normalizer.normalize(
                query == null ? "" : query.trim(), Normalizer.Form.NFKC);
        int length = normalized.codePointCount(0, normalized.length());
        if (length < MIN_QUERY_LENGTH) {
            throw new InvalidSearchQueryException(
                    "Search query must contain at least " + MIN_QUERY_LENGTH + " characters");
        }
        if (length > MAX_QUERY_LENGTH) {
            throw new InvalidSearchQueryException(
                    "Search query must not exceed " + MAX_QUERY_LENGTH + " characters");
        }
        return normalized;
    }

    private void requireUser(UUID userId) {
        if (!userRepository.existsById(userId)) throw new UserNotFoundException();
    }

    private Pageable normalize(Pageable pageable, Sort sort) {
        int size = Math.max(1, Math.min(pageable.getPageSize(), MAX_PAGE_SIZE));
        return PageRequest.of(pageable.getPageNumber(), size, sort);
    }
}
