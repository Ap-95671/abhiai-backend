package com.abhiai.abhiai_backend.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.abhiai.abhiai_backend.entity.Hashtag;
import com.abhiai.abhiai_backend.entity.Post;
import com.abhiai.abhiai_backend.entity.User;
import com.abhiai.abhiai_backend.repository.HashtagRepository;
import com.abhiai.abhiai_backend.repository.PostRepository;
import com.abhiai.abhiai_backend.repository.UserRepository;

@Component
public class PostgresSearchProvider implements SearchProvider {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final HashtagRepository hashtagRepository;

    public PostgresSearchProvider(
            UserRepository userRepository,
            PostRepository postRepository,
            HashtagRepository hashtagRepository) {
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.hashtagRepository = hashtagRepository;
    }

    @Override
    public Page<User> searchUsers(String query, Pageable pageable) {
        return userRepository.searchByUsernameOrDisplayName(query, pageable);
    }

    @Override
    public Page<Post> searchPosts(
            UUID actingUserId,
            String query,
            PostSearchCriteria criteria,
            Pageable pageable) {
        return postRepository.searchVisiblePosts(
                actingUserId,
                query,
                criteria.authorUsername(),
                criteria.fromDate(),
                criteria.toDate(),
                criteria.hasMedia(),
                criteria.sort().name(),
                pageable);
    }

    @Override
    public Page<Hashtag> searchHashtags(String query, Pageable pageable) {
        return hashtagRepository.search(query, pageable);
    }
}
