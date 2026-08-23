package com.abhiai.abhiai_backend.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import com.abhiai.abhiai_backend.entity.Hashtag;
import com.abhiai.abhiai_backend.entity.Post;
import com.abhiai.abhiai_backend.entity.User;
import com.abhiai.abhiai_backend.repository.HashtagRepository;
import com.abhiai.abhiai_backend.repository.PostRepository;
import com.abhiai.abhiai_backend.repository.UserRepository;

@Component
public class PostgresExploreRankingProvider implements ExploreRankingProvider {

    private static final Sort HASHTAG_SORT = Sort.by(
            Sort.Order.desc("postCount"),
            Sort.Order.desc("createdAt"),
            Sort.Order.asc("id"));

    private final PostRepository postRepository;
    private final HashtagRepository hashtagRepository;
    private final UserRepository userRepository;

    public PostgresExploreRankingProvider(
            PostRepository postRepository,
            HashtagRepository hashtagRepository,
            UserRepository userRepository) {
        this.postRepository = postRepository;
        this.hashtagRepository = hashtagRepository;
        this.userRepository = userRepository;
    }

    @Override
    public List<Post> trendingPosts(Instant cutoff, int limit) {
        return postRepository.findTrendingPublicPosts(cutoff, PageRequest.of(0, limit));
    }

    @Override
    public List<Hashtag> trendingHashtags(int limit) {
        return hashtagRepository.findTrendingRecent(PageRequest.of(0, limit));
    }

    @Override
    public List<User> suggestedAccounts(UUID actingUserId, int limit) {
        return userRepository.findSuggestedAccounts(actingUserId, PageRequest.of(0, limit));
    }

    @Override
    public List<Post> popularDiscussions(Instant cutoff, int limit) {
        return postRepository.findPopularPublicDiscussions(cutoff, PageRequest.of(0, limit));
    }

    @Override
    public List<Post> recommendedMedia(Instant cutoff, int limit) {
        return postRepository.findRecommendedPublicMedia(cutoff, PageRequest.of(0, limit));
    }
}
