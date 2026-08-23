package com.abhiai.abhiai_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.abhiai.abhiai_backend.dto.community.CreateCommunityPostRequest;
import com.abhiai.abhiai_backend.dto.community.CreateCommunityRequest;
import com.abhiai.abhiai_backend.entity.Community;
import com.abhiai.abhiai_backend.entity.CommunityMembership;
import com.abhiai.abhiai_backend.entity.CommunityPrivacy;
import com.abhiai.abhiai_backend.entity.CommunityRole;
import com.abhiai.abhiai_backend.entity.User;
import com.abhiai.abhiai_backend.exception.CommunitySlugAlreadyExistsException;
import com.abhiai.abhiai_backend.exception.DuplicateCommunityMembershipException;
import com.abhiai.abhiai_backend.exception.InvalidCommunityException;
import com.abhiai.abhiai_backend.exception.UnauthorizedActionException;
import com.abhiai.abhiai_backend.repository.CommunityMembershipRepository;
import com.abhiai.abhiai_backend.repository.CommunityRepository;
import com.abhiai.abhiai_backend.repository.PostRepository;
import com.abhiai.abhiai_backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class CommunityServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID COMMUNITY_ID = UUID.randomUUID();

    @Mock CommunityRepository communityRepository;
    @Mock CommunityMembershipRepository membershipRepository;
    @Mock PostRepository postRepository;
    @Mock UserRepository userRepository;
    @Mock PostService postService;

    private CommunityService service;
    private User user;
    private Community community;

    @BeforeEach
    void setUp() {
        service = new CommunityService(
                communityRepository, membershipRepository, postRepository, userRepository, postService);
        user = new User("builder", "Builder", "builder@example.com", "hash");
        ReflectionTestUtils.setField(user, "id", USER_ID);
        community = new Community(
                "AI Builders", "ai-builders", "Build useful AI", null, null, user, CommunityPrivacy.PUBLIC);
        ReflectionTestUtils.setField(community, "id", COMMUNITY_ID);
    }

    @Test
    void createsPublicCommunityAndOwnerMembership() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(communityRepository.saveAndFlush(any(Community.class)))
                .thenAnswer(invocation -> {
                    Community saved = invocation.getArgument(0);
                    ReflectionTestUtils.setField(saved, "id", COMMUNITY_ID);
                    return saved;
                });

        var response = service.createCommunity(USER_ID, new CreateCommunityRequest(
                "  AI   Builders ", null, " Build useful AI ", null, null, null));

        assertEquals("ai-builders", response.slug());
        assertEquals(1, response.memberCount());
        assertEquals(CommunityRole.OWNER, response.currentUserRole());
        ArgumentCaptor<CommunityMembership> membership = ArgumentCaptor.forClass(CommunityMembership.class);
        verify(membershipRepository).saveAndFlush(membership.capture());
        assertEquals(CommunityRole.OWNER, membership.getValue().getRole());
    }

    @Test
    void rejectsPrivateCommunitiesUntilApprovalSupportExists() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        assertThrows(InvalidCommunityException.class, () -> service.createCommunity(
                USER_ID,
                new CreateCommunityRequest("Private", "private-ai", "Later", null, null,
                        CommunityPrivacy.PRIVATE)));

        verify(communityRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsDuplicateSlug() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(communityRepository.existsBySlugIgnoreCase("ai-builders")).thenReturn(true);

        assertThrows(CommunitySlugAlreadyExistsException.class, () -> service.createCommunity(
                USER_ID,
                new CreateCommunityRequest("AI Builders", "ai-builders", "Build", null, null, null)));
    }

    @Test
    void joinsCommunityAndUpdatesCounter() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(communityRepository.findBySlugForUpdate("ai-builders")).thenReturn(Optional.of(community));

        var response = service.joinCommunity(USER_ID, "AI-Builders");

        assertEquals(2, response.memberCount());
        assertTrue(response.joined());
        assertEquals(CommunityRole.MEMBER, response.currentUserRole());
    }

    @Test
    void rejectsDuplicateMembership() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(communityRepository.findBySlugForUpdate("ai-builders")).thenReturn(Optional.of(community));
        when(membershipRepository.existsByCommunityIdAndUserId(COMMUNITY_ID, USER_ID)).thenReturn(true);

        assertThrows(DuplicateCommunityMembershipException.class,
                () -> service.joinCommunity(USER_ID, "ai-builders"));
    }

    @Test
    void memberCanLeaveButOwnerCannot() {
        when(communityRepository.findBySlugForUpdate("ai-builders")).thenReturn(Optional.of(community));
        CommunityMembership member = new CommunityMembership(community, user, CommunityRole.MEMBER);
        when(membershipRepository.findByCommunityIdAndUserId(COMMUNITY_ID, USER_ID))
                .thenReturn(Optional.of(member));
        community.addMember();

        var response = service.leaveCommunity(USER_ID, "ai-builders");

        assertEquals(1, response.memberCount());
        assertFalse(response.joined());

        CommunityMembership owner = new CommunityMembership(community, user, CommunityRole.OWNER);
        when(membershipRepository.findByCommunityIdAndUserId(COMMUNITY_ID, USER_ID))
                .thenReturn(Optional.of(owner));
        assertThrows(InvalidCommunityException.class,
                () -> service.leaveCommunity(USER_ID, "ai-builders"));
    }

    @Test
    void onlyMembersCanPublish() {
        when(communityRepository.findBySlugIgnoreCase("ai-builders")).thenReturn(Optional.of(community));
        CreateCommunityPostRequest request = new CreateCommunityPostRequest("Hello community", List.of());

        assertThrows(UnauthorizedActionException.class,
                () -> service.publishPost(USER_ID, "ai-builders", request));
        verify(postService, never()).createCommunityPost(any(), any(), any(), any());
    }
}
