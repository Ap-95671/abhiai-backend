package com.abhiai.abhiai_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import com.abhiai.abhiai_backend.entity.User;
import com.abhiai.abhiai_backend.exception.UnauthorizedActionException;
import com.abhiai.abhiai_backend.repository.PostLikeRepository;
import com.abhiai.abhiai_backend.repository.PostReplyRepository;
import com.abhiai.abhiai_backend.repository.PostRepository;
import com.abhiai.abhiai_backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class ProfileContentServiceTest {

    private static final UUID VIEWER_ID = UUID.randomUUID();
    private static final UUID PROFILE_ID = UUID.randomUUID();

    @Mock UserRepository userRepository;
    @Mock PostRepository postRepository;
    @Mock PostReplyRepository replyRepository;
    @Mock PostLikeRepository likeRepository;
    @Mock UsernamePolicy usernamePolicy;

    private ProfileContentService service;
    private User profileUser;

    @BeforeEach
    void setUp() {
        service = new ProfileContentService(
                userRepository, postRepository, replyRepository, likeRepository, usernamePolicy);
        profileUser = new User("builder", "Builder", "builder@example.com", "hash");
        ReflectionTestUtils.setField(profileUser, "id", PROFILE_ID);
        when(usernamePolicy.normalizeAndValidate("Builder")).thenReturn("builder");
        when(userRepository.findByUsernameIgnoreCase("builder")).thenReturn(Optional.of(profileUser));
    }

    @Test
    void loadsPrivacyFilteredProfilePosts() {
        PageRequest pageable = PageRequest.of(0, 20);
        when(postRepository.findVisibleProfilePosts(VIEWER_ID, PROFILE_ID, pageable))
                .thenReturn(Page.empty(pageable));

        var response = service.getPosts(VIEWER_ID, "Builder", pageable);

        assertEquals(0, response.totalElements());
        verify(postRepository).findVisibleProfilePosts(VIEWER_ID, PROFILE_ID, pageable);
    }

    @Test
    void loadsPrivacyFilteredRepliesAndMedia() {
        PageRequest pageable = PageRequest.of(0, 20);
        when(replyRepository.findVisibleProfileReplies(VIEWER_ID, PROFILE_ID, pageable))
                .thenReturn(Page.empty(pageable));
        when(postRepository.findVisibleProfileMediaPosts(VIEWER_ID, PROFILE_ID, pageable))
                .thenReturn(Page.empty(pageable));

        assertEquals(0, service.getReplies(VIEWER_ID, "Builder", pageable).totalElements());
        assertEquals(0, service.getMedia(VIEWER_ID, "Builder", pageable).totalElements());
    }

    @Test
    void hidesLikesFromOtherUsersWhenPrivacySettingIsOff() {
        ReflectionTestUtils.setField(profileUser, "showLikesOnProfile", false);

        assertThrows(UnauthorizedActionException.class,
                () -> service.getLikes(VIEWER_ID, "Builder", PageRequest.of(0, 20)));

        verify(likeRepository, never()).findVisibleProfileLikedPosts(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void ownerCanAlwaysViewOwnLikes() {
        ReflectionTestUtils.setField(profileUser, "showLikesOnProfile", false);
        PageRequest pageable = PageRequest.of(0, 20);
        when(likeRepository.findVisibleProfileLikedPosts(PROFILE_ID, PROFILE_ID, pageable))
                .thenReturn(Page.empty(pageable));

        assertEquals(0, service.getLikes(PROFILE_ID, "Builder", pageable).totalElements());
    }
}
