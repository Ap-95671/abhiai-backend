package com.abhiai.abhiai_backend.service;

import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.abhiai.abhiai_backend.dto.common.PageResponse;
import com.abhiai.abhiai_backend.dto.post.PostResponse;
import com.abhiai.abhiai_backend.dto.user.ProfileReplyResponse;
import com.abhiai.abhiai_backend.entity.User;
import com.abhiai.abhiai_backend.exception.UnauthorizedActionException;
import com.abhiai.abhiai_backend.exception.UserNotFoundException;
import com.abhiai.abhiai_backend.repository.PostLikeRepository;
import com.abhiai.abhiai_backend.repository.PostReplyRepository;
import com.abhiai.abhiai_backend.repository.PostRepository;
import com.abhiai.abhiai_backend.repository.UserRepository;

@Service
public class ProfileContentService {

    private static final int MAX_PAGE_SIZE = 100;

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final PostReplyRepository replyRepository;
    private final PostLikeRepository likeRepository;
    private final UsernamePolicy usernamePolicy;
    private final BlockPolicyService blockPolicyService;

    public ProfileContentService(
            UserRepository userRepository,
            PostRepository postRepository,
            PostReplyRepository replyRepository,
            PostLikeRepository likeRepository,
            UsernamePolicy usernamePolicy) {
        this(userRepository, postRepository, replyRepository, likeRepository, usernamePolicy, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public ProfileContentService(UserRepository userRepository, PostRepository postRepository,
            PostReplyRepository replyRepository, PostLikeRepository likeRepository,
            UsernamePolicy usernamePolicy, BlockPolicyService blockPolicyService) {
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.replyRepository = replyRepository;
        this.likeRepository = likeRepository;
        this.usernamePolicy = usernamePolicy;
        this.blockPolicyService = blockPolicyService;
    }

    @Transactional(readOnly = true)
    public PageResponse<PostResponse> getPosts(UUID viewerId, String username, Pageable pageable) {
        User profileUser = findProfileUser(username);
        requireVisible(viewerId, profileUser.getId());
        Pageable normalized = normalize(pageable);
        if (profileUser.getId().equals(viewerId)) {
            return PageResponse.from(
                    postRepository.findByAuthorIdAndDeletedAtIsNullOrderByPinnedAtDescCreatedAtDescIdDesc(
                            profileUser.getId(), normalized),
                    PostResponse::from);
        }
        return PageResponse.from(
                postRepository.findVisibleProfilePosts(viewerId, profileUser.getId(), normalized),
                PostResponse::from);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProfileReplyResponse> getReplies(
            UUID viewerId, String username, Pageable pageable) {
        User profileUser = findProfileUser(username);
        requireVisible(viewerId, profileUser.getId());
        return PageResponse.from(
                replyRepository.findVisibleProfileReplies(
                        viewerId, profileUser.getId(), normalize(pageable)),
                ProfileReplyResponse::from);
    }

    @Transactional(readOnly = true)
    public PageResponse<PostResponse> getMedia(UUID viewerId, String username, Pageable pageable) {
        User profileUser = findProfileUser(username);
        requireVisible(viewerId, profileUser.getId());
        return PageResponse.from(
                postRepository.findVisibleProfileMediaPosts(
                        viewerId, profileUser.getId(), normalize(pageable)),
                PostResponse::from);
    }

    @Transactional(readOnly = true)
    public PageResponse<PostResponse> getLikes(UUID viewerId, String username, Pageable pageable) {
        User profileUser = findProfileUser(username);
        requireVisible(viewerId, profileUser.getId());
        if (!profileUser.getId().equals(viewerId) && !profileUser.isShowLikesOnProfile()) {
            throw new UnauthorizedActionException("This user has hidden their liked posts");
        }
        return PageResponse.from(
                likeRepository.findVisibleProfileLikedPosts(
                        viewerId, profileUser.getId(), normalize(pageable)),
                PostResponse::from);
    }

    private User findProfileUser(String username) {
        String normalized = usernamePolicy.normalizeAndValidate(username);
        return userRepository.findByUsernameIgnoreCase(normalized)
                .orElseThrow(UserNotFoundException::new);
    }

    private void requireVisible(UUID viewerId, UUID profileUserId) {
        if (blockPolicyService != null && blockPolicyService.isBlockedEitherDirection(viewerId, profileUserId)) {
            throw new UserNotFoundException();
        }
    }

    private Pageable normalize(Pageable pageable) {
        int page = Math.max(0, pageable.getPageNumber());
        int size = Math.max(1, Math.min(pageable.getPageSize(), MAX_PAGE_SIZE));
        return PageRequest.of(page, size);
    }
}
