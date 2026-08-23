package com.abhiai.abhiai_backend.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.abhiai.abhiai_backend.dto.bookmark.BookmarkedPostResponse;
import com.abhiai.abhiai_backend.dto.common.PageResponse;
import com.abhiai.abhiai_backend.security.JwtPrincipal;
import com.abhiai.abhiai_backend.service.PostBookmarkService;

@RestController
@RequestMapping("/api/v1/bookmarks")
public class BookmarkController {

    private final PostBookmarkService postBookmarkService;

    public BookmarkController(PostBookmarkService postBookmarkService) {
        this.postBookmarkService = postBookmarkService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<BookmarkedPostResponse>> getBookmarks(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(postBookmarkService.getBookmarks(principal.userId(), pageable));
    }
}
