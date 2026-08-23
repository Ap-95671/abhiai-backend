package com.abhiai.abhiai_backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.abhiai.abhiai_backend.dto.explore.ExploreResponse;
import com.abhiai.abhiai_backend.security.JwtPrincipal;
import com.abhiai.abhiai_backend.service.ExploreService;

@RestController
@RequestMapping("/api/v1/explore")
public class ExploreController {

    private final ExploreService exploreService;

    public ExploreController(ExploreService exploreService) {
        this.exploreService = exploreService;
    }

    @GetMapping
    public ResponseEntity<ExploreResponse> explore(
            @AuthenticationPrincipal JwtPrincipal principal,
            @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(exploreService.explore(principal.userId(), limit));
    }
}
