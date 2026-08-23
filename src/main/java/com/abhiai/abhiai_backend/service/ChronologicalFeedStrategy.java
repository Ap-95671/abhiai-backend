package com.abhiai.abhiai_backend.service;

import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.abhiai.abhiai_backend.entity.Post;
import com.abhiai.abhiai_backend.entity.PostVisibility;
import com.abhiai.abhiai_backend.repository.PostRepository;

@Component
public class ChronologicalFeedStrategy implements FeedStrategy {

    private static final Set<PostVisibility> FOLLOWED_VISIBILITIES =
            Set.of(PostVisibility.PUBLIC, PostVisibility.FOLLOWERS);

    private final PostRepository postRepository;

    public ChronologicalFeedStrategy(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    @Override
    public Page<Post> load(UUID userId, Pageable pageable) {
        return postRepository.findChronologicalHomeFeed(
                userId,
                FOLLOWED_VISIBILITIES,
                pageable);
    }
}
