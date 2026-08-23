package com.abhiai.abhiai_backend.dto.explore;

import java.time.Instant;
import java.util.List;

import com.abhiai.abhiai_backend.dto.hashtag.HashtagResponse;
import com.abhiai.abhiai_backend.dto.post.PostResponse;
import com.abhiai.abhiai_backend.dto.search.UserSearchResponse;

public record ExploreResponse(
        List<PostResponse> trendingPosts,
        List<HashtagResponse> trendingHashtags,
        List<UserSearchResponse> suggestedAccounts,
        List<PostResponse> popularDiscussions,
        List<PostResponse> recommendedMedia,
        String rankingSummary,
        int rankingWindowDays,
        Instant generatedAt) {
}
