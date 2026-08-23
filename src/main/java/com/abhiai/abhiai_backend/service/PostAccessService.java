package com.abhiai.abhiai_backend.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.abhiai.abhiai_backend.entity.Post;
import com.abhiai.abhiai_backend.entity.PostVisibility;
import com.abhiai.abhiai_backend.exception.PostNotFoundException;
import com.abhiai.abhiai_backend.exception.UnauthorizedActionException;
import com.abhiai.abhiai_backend.repository.FollowRepository;
import com.abhiai.abhiai_backend.repository.PostRepository;

@Service
public class PostAccessService {

    private final PostRepository postRepository;
    private final FollowRepository followRepository;
    private final BlockPolicyService blockPolicyService;

    public PostAccessService(PostRepository postRepository, FollowRepository followRepository) {
        this(postRepository, followRepository, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public PostAccessService(PostRepository postRepository, FollowRepository followRepository,
            BlockPolicyService blockPolicyService) {
        this.postRepository = postRepository;
        this.followRepository = followRepository;
        this.blockPolicyService = blockPolicyService;
    }

    public Post findActivePost(UUID postId) {
        return postRepository.findByIdAndDeletedAtIsNull(postId)
                .orElseThrow(PostNotFoundException::new);
    }

    public Post findViewablePost(UUID actingUserId, UUID postId) {
        Post post = findActivePost(postId);
        UUID authorId = post.getAuthor().getId();
        if (blockPolicyService != null) blockPolicyService.requireInteractionAllowed(actingUserId, authorId);

        if (actingUserId.equals(authorId) || post.getVisibility() == PostVisibility.PUBLIC) {
            return post;
        }
        if (post.getVisibility() == PostVisibility.FOLLOWERS
                && followRepository.existsByFollowerIdAndFollowingId(actingUserId, authorId)) {
            return post;
        }
        throw new UnauthorizedActionException("You are not allowed to view this post");
    }
}
