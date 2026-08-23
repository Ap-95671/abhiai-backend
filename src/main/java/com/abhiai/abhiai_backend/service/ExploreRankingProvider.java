package com.abhiai.abhiai_backend.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.abhiai.abhiai_backend.entity.Hashtag;
import com.abhiai.abhiai_backend.entity.Post;
import com.abhiai.abhiai_backend.entity.User;

public interface ExploreRankingProvider {

    List<Post> trendingPosts(Instant cutoff, int limit);

    List<Hashtag> trendingHashtags(int limit);

    List<User> suggestedAccounts(UUID actingUserId, int limit);

    List<Post> popularDiscussions(Instant cutoff, int limit);

    List<Post> recommendedMedia(Instant cutoff, int limit);
}
