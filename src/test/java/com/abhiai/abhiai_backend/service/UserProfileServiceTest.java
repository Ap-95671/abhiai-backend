package com.abhiai.abhiai_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.eq;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.abhiai.abhiai_backend.dto.user.UpdateUserProfileRequest;
import com.abhiai.abhiai_backend.dto.user.UserProfileResponse;
import com.abhiai.abhiai_backend.entity.User;
import com.abhiai.abhiai_backend.entity.MediaAsset;
import com.abhiai.abhiai_backend.exception.UserNotFoundException;
import com.abhiai.abhiai_backend.exception.UsernameAlreadyTakenException;
import com.abhiai.abhiai_backend.repository.UserRepository;
import com.abhiai.abhiai_backend.repository.MediaAssetRepository;
import com.abhiai.abhiai_backend.repository.PostRepository;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private UserRepository userRepository;

    @Mock
    private UsernamePolicy usernamePolicy;
    @Mock
    private MediaAssetRepository mediaAssetRepository;
    @Mock
    private BlockPolicyService blockPolicyService;
    @Mock
    private PostRepository postRepository;

    private UserProfileService userProfileService;

    @BeforeEach
    void setUp() {
        userProfileService = new UserProfileService(userRepository, usernamePolicy, mediaAssetRepository, blockPolicyService, postRepository);
    }

    @Test
    void getsAProfileByCaseInsensitiveNormalizedUsername() {
        User user = user();
        when(usernamePolicy.normalizeAndValidate(" Abhishek ")).thenReturn("abhishek");
        when(userRepository.findByUsernameIgnoreCase("abhishek")).thenReturn(Optional.of(user));

        UserProfileResponse response = userProfileService.getByUsername(" Abhishek ");

        assertEquals("abhishek", response.username());
        assertEquals("Abhishek", response.displayName());
    }

    @Test
    void reportsThePostCountVisibleToTheCurrentViewer() {
        User user = user();
        UUID viewerId = UUID.randomUUID();
        when(usernamePolicy.normalizeAndValidate("abhishek")).thenReturn("abhishek");
        when(userRepository.findByUsernameIgnoreCase("abhishek")).thenReturn(Optional.of(user));
        when(postRepository.findVisibleProfilePosts(eq(viewerId), eq(user.getId()), eq(PageRequest.of(0, 1))))
                .thenReturn(new PageImpl<>(java.util.List.of(), PageRequest.of(0, 1), 2));

        UserProfileResponse response = userProfileService.getByUsername(viewerId, "abhishek");

        assertEquals(2, response.postCount());
    }

    @Test
    void updatesOnlyTheAuthenticatedUsersProfile() {
        User user = user();
        UpdateUserProfileRequest request = new UpdateUserProfileRequest(
                "New_Name",
                "  New display name  ",
                " Building AbhiAI. ",
                "",
                null,
                null,
                null,
                " India ",
                "https://example.com",
                LocalDate.of(2000, 1, 1),
                null);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(usernamePolicy.normalizeAndValidate("New_Name")).thenReturn("new_name");
        when(userRepository.existsByUsernameIgnoreCaseAndIdNot("new_name", USER_ID)).thenReturn(false);
        when(userRepository.saveAndFlush(user)).thenReturn(user);

        UserProfileResponse response = userProfileService.updateCurrentUser(USER_ID, request);

        assertEquals("new_name", response.username());
        assertEquals("New display name", response.displayName());
        assertEquals("Building AbhiAI.", response.bio());
        assertEquals("India", response.location());
        verify(userRepository).findById(USER_ID);
        verify(userRepository).saveAndFlush(user);
    }

    @Test
    void rejectsAUsernameOwnedByAnotherUser() {
        User user = user();
        UpdateUserProfileRequest request = new UpdateUserProfileRequest(
                "taken", null, null, null, null, null, null, null, null, null, null);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(usernamePolicy.normalizeAndValidate("taken")).thenReturn("taken");
        when(userRepository.existsByUsernameIgnoreCaseAndIdNot("taken", USER_ID)).thenReturn(true);

        assertThrows(
                UsernameAlreadyTakenException.class,
                () -> userProfileService.updateCurrentUser(USER_ID, request));

        verify(userRepository, never()).saveAndFlush(user);
    }

    @Test
    void assignsAnOwnedUploadedImageToTheProfile() {
        User user = user();
        UUID mediaId = UUID.randomUUID();
        MediaAsset asset = new MediaAsset(mediaId, user, mediaId + ".png", "avatar.png", "image/png", 128);
        UpdateUserProfileRequest request = new UpdateUserProfileRequest(
                null, null, null, null, null, mediaId, null, null, null, null, null);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(mediaAssetRepository.findByIdAndOwnerIdAndPostIsNull(mediaId, USER_ID)).thenReturn(Optional.of(asset));
        when(userRepository.saveAndFlush(user)).thenReturn(user);

        UserProfileResponse response = userProfileService.updateCurrentUser(USER_ID, request);

        assertEquals(mediaId, response.profileMediaId());
        assertEquals(asset, user.getProfileMedia());
    }

    @Test
    void returnsNotFoundForAnUnknownAuthenticatedUser() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userProfileService.getCurrentUser(USER_ID));
    }

    private User user() {
        return new User("abhishek", "Abhishek", "user@example.com", "hash");
    }
}
