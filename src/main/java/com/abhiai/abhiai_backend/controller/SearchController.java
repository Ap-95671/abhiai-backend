package com.abhiai.abhiai_backend.controller;

import java.time.Instant;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.abhiai.abhiai_backend.dto.common.PageResponse;
import com.abhiai.abhiai_backend.dto.hashtag.HashtagResponse;
import com.abhiai.abhiai_backend.dto.post.PostResponse;
import com.abhiai.abhiai_backend.dto.search.UserSearchResponse;
import com.abhiai.abhiai_backend.security.JwtPrincipal;
import com.abhiai.abhiai_backend.service.PostSearchCriteria;
import com.abhiai.abhiai_backend.service.SearchService;
import com.abhiai.abhiai_backend.service.SearchSort;
import com.abhiai.abhiai_backend.service.SearchType;

@RestController
@RequestMapping("/api/v1/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    public ResponseEntity<?> search(
            @AuthenticationPrincipal JwtPrincipal principal,
            @RequestParam("q") String query,
            @RequestParam(defaultValue = "USERS") SearchType type,
            @RequestParam(required = false) String user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) Boolean hasMedia,
            @RequestParam(defaultValue = "RELEVANCE") SearchSort sort,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(switch (type) {
            case USERS -> searchService.searchUsers(query, pageable);
            case HASHTAGS -> searchService.searchHashtags(query, pageable);
            case POSTS -> searchService.searchPosts(
                    principal.userId(), query, criteria(user, from, to, hasMedia, sort), pageable);
        });
    }

    @GetMapping("/users")
    public ResponseEntity<PageResponse<UserSearchResponse>> searchUsers(
            @RequestParam("q") String query,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(searchService.searchUsers(query, pageable));
    }

    @GetMapping("/posts")
    public ResponseEntity<PageResponse<PostResponse>> searchPosts(
            @AuthenticationPrincipal JwtPrincipal principal,
            @RequestParam("q") String query,
            @RequestParam(required = false) String user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) Boolean hasMedia,
            @RequestParam(defaultValue = "RELEVANCE") SearchSort sort,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(searchService.searchPosts(
                principal.userId(), query, criteria(user, from, to, hasMedia, sort), pageable));
    }

    @GetMapping("/hashtags")
    public ResponseEntity<PageResponse<HashtagResponse>> searchHashtags(
            @RequestParam("q") String query,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(searchService.searchHashtags(query, pageable));
    }

    private PostSearchCriteria criteria(
            String user,
            Instant from,
            Instant to,
            Boolean hasMedia,
            SearchSort sort) {
        return new PostSearchCriteria(user, from, to, hasMedia, sort);
    }
}
