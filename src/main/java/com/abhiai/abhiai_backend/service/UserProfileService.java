package com.abhiai.abhiai_backend.service;

import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.abhiai.abhiai_backend.dto.user.UpdateUserProfileRequest;
import com.abhiai.abhiai_backend.dto.user.UserProfileResponse;
import com.abhiai.abhiai_backend.entity.User;
import com.abhiai.abhiai_backend.exception.UserNotFoundException;
import com.abhiai.abhiai_backend.exception.UsernameAlreadyTakenException;
import com.abhiai.abhiai_backend.repository.UserRepository;
import com.abhiai.abhiai_backend.repository.PostRepository;
import com.abhiai.abhiai_backend.repository.MediaAssetRepository;
import com.abhiai.abhiai_backend.entity.MediaAsset;
import com.abhiai.abhiai_backend.exception.InvalidMediaException;
import com.abhiai.abhiai_backend.entity.AccountPrivacy;

@Service
public class UserProfileService {

    private final UserRepository userRepository;
    private final UsernamePolicy usernamePolicy;
    private final MediaAssetRepository mediaAssetRepository;
    private final BlockPolicyService blockPolicyService;
    private final PostRepository postRepository;

    public UserProfileService(UserRepository userRepository, UsernamePolicy usernamePolicy, MediaAssetRepository mediaAssetRepository) {
        this(userRepository, usernamePolicy, mediaAssetRepository, null, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public UserProfileService(UserRepository userRepository, UsernamePolicy usernamePolicy,
            MediaAssetRepository mediaAssetRepository, BlockPolicyService blockPolicyService,
            PostRepository postRepository) {
        this.userRepository = userRepository;
        this.usernamePolicy = usernamePolicy;
        this.mediaAssetRepository = mediaAssetRepository;
        this.blockPolicyService = blockPolicyService;
        this.postRepository = postRepository;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getByUsername(String username) {
        String normalizedUsername = usernamePolicy.normalizeAndValidate(username);
        return UserProfileResponse.from(userRepository.findByUsernameIgnoreCase(normalizedUsername)
                .orElseThrow(UserNotFoundException::new));
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getByUsername(UUID viewerId, String username) {
        String normalizedUsername = usernamePolicy.normalizeAndValidate(username);
        User target = userRepository.findByUsernameIgnoreCase(normalizedUsername).orElseThrow(UserNotFoundException::new);
        if (blockPolicyService != null && blockPolicyService.isBlockedEitherDirection(viewerId, target.getId())) {
            throw new UserNotFoundException();
        }
        long visiblePostCount = postRepository == null
                ? target.getPostCount()
                : postRepository.findVisibleProfilePosts(viewerId, target.getId(), PageRequest.of(0, 1))
                        .getTotalElements();
        return UserProfileResponse.from(target, visiblePostCount);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUser(UUID userId) {
        return UserProfileResponse.from(findUser(userId));
    }

    @Transactional
    public UserProfileResponse updateCurrentUser(UUID userId, UpdateUserProfileRequest request) {
        User user = findUser(userId);
        String username = request.username() == null
                ? null
                : usernamePolicy.normalizeAndValidate(request.username());

        if (username != null
                && !username.equalsIgnoreCase(user.getUsername())
                && userRepository.existsByUsernameIgnoreCaseAndIdNot(username, userId)) {
            throw new UsernameAlreadyTakenException(username);
        }

        user.updateProfile(
                username,
                trim(request.displayName()),
                request.bio(),
                request.profilePicture(),
                request.coverPicture(),
                findOwnedMedia(request.profileMediaId(), userId),
                findOwnedMedia(request.coverMediaId(), userId),
                request.location(),
                request.website(),
                request.dateOfBirth(),
                request.showLikesOnProfile());

        try {
            return UserProfileResponse.from(userRepository.saveAndFlush(user));
        } catch (DataIntegrityViolationException exception) {
            if (username != null) {
                throw new UsernameAlreadyTakenException(username);
            }
            throw exception;
        }
    }

    @Transactional public UserProfileResponse updatePrivacy(UUID userId,AccountPrivacy privacy){User user=findUser(userId);user.changeAccountPrivacy(privacy);return UserProfileResponse.from(userRepository.saveAndFlush(user));}

    private MediaAsset findOwnedMedia(UUID mediaId, UUID userId) {
        if (mediaId == null) return null;
        return mediaAssetRepository.findByIdAndOwnerIdAndPostIsNull(mediaId, userId)
                .orElseThrow(() -> new InvalidMediaException("Profile media must be uploaded by you and not attached to a post"));
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
