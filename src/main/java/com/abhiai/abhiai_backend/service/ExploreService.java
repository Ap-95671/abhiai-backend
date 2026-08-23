package com.abhiai.abhiai_backend.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.abhiai.abhiai_backend.dto.explore.ExploreResponse;
import com.abhiai.abhiai_backend.dto.hashtag.HashtagResponse;
import com.abhiai.abhiai_backend.dto.post.PostResponse;
import com.abhiai.abhiai_backend.dto.search.UserSearchResponse;
import com.abhiai.abhiai_backend.exception.UserNotFoundException;
import com.abhiai.abhiai_backend.repository.UserRepository;

@Service
public class ExploreService {

    static final int RANKING_WINDOW_DAYS = 30;
    private static final int DEFAULT_LIMIT = 8;
    private static final int MAX_LIMIT = 20;
    private static final String RANKING_SUMMARY =
            "Ranked from recent public activity using likes, replies, reposts, views, and recency.";

    private final UserRepository userRepository;
    private final ExploreRankingProvider rankingProvider;
    private final Clock clock;

    @Autowired
    public ExploreService(UserRepository userRepository, ExploreRankingProvider rankingProvider) {
        this(userRepository, rankingProvider, Clock.systemUTC());
    }

    ExploreService(UserRepository userRepository, ExploreRankingProvider rankingProvider, Clock clock) {
        this.userRepository = userRepository;
        this.rankingProvider = rankingProvider;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ExploreResponse explore(UUID actingUserId, Integer requestedLimit) {
        if (!userRepository.existsById(actingUserId)) throw new UserNotFoundException();

        int limit = normalizeLimit(requestedLimit);
        Instant generatedAt = clock.instant();
        Instant cutoff = generatedAt.minus(Duration.ofDays(RANKING_WINDOW_DAYS));

        return new ExploreResponse(
                rankingProvider.trendingPosts(cutoff, limit).stream().map(PostResponse::from).toList(),
                rankingProvider.trendingHashtags(limit).stream().map(HashtagResponse::from).toList(),
                rankingProvider.suggestedAccounts(actingUserId, limit).stream()
                        .map(UserSearchResponse::from).toList(),
                rankingProvider.popularDiscussions(cutoff, limit).stream().map(PostResponse::from).toList(),
                rankingProvider.recommendedMedia(cutoff, limit).stream().map(PostResponse::from).toList(),
                RANKING_SUMMARY,
                RANKING_WINDOW_DAYS,
                generatedAt);
    }

    private int normalizeLimit(Integer requestedLimit) {
        if (requestedLimit == null) return DEFAULT_LIMIT;
        return Math.max(1, Math.min(requestedLimit, MAX_LIMIT));
    }
}
