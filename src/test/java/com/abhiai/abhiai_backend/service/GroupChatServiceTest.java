package com.abhiai.abhiai_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
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

import com.abhiai.abhiai_backend.entity.GroupConversation;
import com.abhiai.abhiai_backend.entity.GroupInvitation;
import com.abhiai.abhiai_backend.entity.GroupInvitationStatus;
import com.abhiai.abhiai_backend.entity.GroupMessage;
import com.abhiai.abhiai_backend.entity.GroupParticipant;
import com.abhiai.abhiai_backend.entity.GroupRole;
import com.abhiai.abhiai_backend.entity.User;
import com.abhiai.abhiai_backend.exception.InvalidGroupActionException;
import com.abhiai.abhiai_backend.exception.UnauthorizedActionException;
import com.abhiai.abhiai_backend.repository.GroupConversationRepository;
import com.abhiai.abhiai_backend.repository.GroupInvitationRepository;
import com.abhiai.abhiai_backend.repository.GroupMessageRepository;
import com.abhiai.abhiai_backend.repository.GroupParticipantRepository;
import com.abhiai.abhiai_backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class GroupChatServiceTest {

    private static final UUID OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID MEMBER_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID OUTSIDER_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID GROUP_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final Instant NOW = Instant.parse("2026-08-17T17:00:00Z");

    @Mock private GroupConversationRepository conversationRepository;
    @Mock private GroupParticipantRepository participantRepository;
    @Mock private GroupInvitationRepository invitationRepository;
    @Mock private GroupMessageRepository messageRepository;
    @Mock private UserRepository userRepository;

    private GroupChatService service;
    private User owner;
    private User admin;
    private User member;
    private User outsider;
    private GroupConversation group;
    private GroupParticipant ownerParticipant;
    private GroupParticipant adminParticipant;
    private GroupParticipant memberParticipant;

    @BeforeEach
    void setUp() {
        service = new GroupChatService(
                conversationRepository,
                participantRepository,
                invitationRepository,
                messageRepository,
                userRepository,
                Clock.fixed(NOW, ZoneOffset.UTC));
        owner = user(OWNER_ID, "owner", "Owner");
        admin = user(ADMIN_ID, "admin", "Admin");
        member = user(MEMBER_ID, "member", "Member");
        outsider = user(OUTSIDER_ID, "outsider", "Outsider");
        group = group();
        ownerParticipant = group.getParticipants().getFirst();
        adminParticipant = addParticipant(admin, GroupRole.ADMIN);
        memberParticipant = addParticipant(member, GroupRole.MEMBER);
    }

    @Test
    void createsGroupWithNormalizedNameAndOwnerMembership() {
        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));
        when(conversationRepository.saveAndFlush(any(GroupConversation.class)))
                .thenAnswer(invocation -> initialized(invocation.getArgument(0)));

        var result = service.createGroup(OWNER_ID, "  Builders   Circle ", "");

        assertEquals("Builders Circle", result.name());
        assertEquals(GroupRole.OWNER, result.currentUserRole());
        assertEquals(1, result.memberCount());
    }

    @Test
    void ordinaryMemberCannotInviteUsers() {
        accessible(MEMBER_ID, memberParticipant);

        assertThrows(UnauthorizedActionException.class,
                () -> service.inviteMember(MEMBER_ID, GROUP_ID, outsider.getUsername()));
        verify(invitationRepository, never()).saveAndFlush(any());
    }

    @Test
    void administratorCanInviteANonMember() {
        accessible(ADMIN_ID, adminParticipant);
        when(userRepository.findByUsernameIgnoreCase("outsider")).thenReturn(Optional.of(outsider));
        when(participantRepository.existsByConversationIdAndUserId(GROUP_ID, OUTSIDER_ID))
                .thenReturn(false);
        when(invitationRepository.findByConversationIdAndInviteeId(GROUP_ID, OUTSIDER_ID))
                .thenReturn(Optional.empty());
        when(invitationRepository.saveAndFlush(any(GroupInvitation.class)))
                .thenAnswer(invocation -> initialized(invocation.getArgument(0)));

        var result = service.inviteMember(ADMIN_ID, GROUP_ID, "@Outsider");

        assertEquals(GroupInvitationStatus.PENDING, result.status());
        assertEquals("admin", result.inviter().username());
    }

    @Test
    void inviteeCanAcceptPendingInvitation() {
        GroupInvitation invitation = initialized(new GroupInvitation(group, owner, outsider));
        when(invitationRepository.findByIdAndInviteeId(invitation.getId(), OUTSIDER_ID))
                .thenReturn(Optional.of(invitation));
        when(participantRepository.existsByConversationIdAndUserId(GROUP_ID, OUTSIDER_ID))
                .thenReturn(false);
        var result = service.acceptInvitation(OUTSIDER_ID, invitation.getId());

        assertEquals(GroupRole.MEMBER, result.currentUserRole());
        assertEquals(GroupInvitationStatus.ACCEPTED, invitation.getStatus());
        assertEquals(4, group.getParticipants().size());
    }

    @Test
    void onlyOwnerCanPromoteAnotherMember() {
        accessible(ADMIN_ID, adminParticipant);

        assertThrows(UnauthorizedActionException.class,
                () -> service.updateMemberRole(ADMIN_ID, GROUP_ID, MEMBER_ID, GroupRole.ADMIN));
    }

    @Test
    void transferringOwnershipDemotesPreviousOwnerToAdmin() {
        accessible(OWNER_ID, ownerParticipant);
        when(participantRepository.findByConversationIdAndUserId(GROUP_ID, MEMBER_ID))
                .thenReturn(Optional.of(memberParticipant));

        var result = service.updateMemberRole(OWNER_ID, GROUP_ID, MEMBER_ID, GroupRole.OWNER);

        assertEquals(MEMBER_ID, result.owner().id());
        assertEquals(GroupRole.ADMIN, ownerParticipant.getRole());
        assertEquals(GroupRole.OWNER, memberParticipant.getRole());
    }

    @Test
    void administratorCannotRemoveAnotherAdministrator() {
        accessible(ADMIN_ID, adminParticipant);
        when(participantRepository.findByConversationIdAndUserId(GROUP_ID, OWNER_ID))
                .thenReturn(Optional.of(ownerParticipant));

        assertThrows(UnauthorizedActionException.class,
                () -> service.removeMember(ADMIN_ID, GROUP_ID, OWNER_ID));
        assertEquals(3, group.getParticipants().size());
    }

    @Test
    void ownerMustTransferOwnershipBeforeLeavingPopulatedGroup() {
        accessible(OWNER_ID, ownerParticipant);
        when(participantRepository.countByConversationId(GROUP_ID)).thenReturn(3L);

        assertThrows(InvalidGroupActionException.class,
                () -> service.leaveGroup(OWNER_ID, GROUP_ID));
        verify(conversationRepository, never()).delete(any());
    }

    @Test
    void administratorCanModerateAnotherMembersMessage() {
        GroupMessage message = initialized(new GroupMessage(group, member, "moderate this"));
        accessible(ADMIN_ID, adminParticipant);
        when(messageRepository.findByIdAndConversationId(message.getId(), GROUP_ID))
                .thenReturn(Optional.of(message));

        var result = service.deleteMessage(ADMIN_ID, GROUP_ID, message.getId());

        assertEquals(true, result.deleted());
        assertEquals(null, result.content());
    }

    private void accessible(UUID userId, GroupParticipant participant) {
        when(conversationRepository.findAccessible(GROUP_ID, userId)).thenReturn(Optional.of(group));
        when(participantRepository.findByConversationIdAndUserId(GROUP_ID, userId))
                .thenReturn(Optional.of(participant));
    }

    private User user(UUID id, String username, String displayName) {
        User value = new User(username, displayName, username + "@example.com", "hash");
        ReflectionTestUtils.setField(value, "id", id);
        return value;
    }

    private GroupConversation group() {
        return initialized(new GroupConversation("Phase 19", null, owner));
    }

    private GroupParticipant addParticipant(User user, GroupRole role) {
        GroupParticipant participant = initialized(new GroupParticipant(group, user, role));
        @SuppressWarnings("unchecked")
        List<GroupParticipant> participants = (List<GroupParticipant>) ReflectionTestUtils
                .getField(group, "participants");
        participants.add(participant);
        return participant;
    }

    private <T> T initialized(T entity) {
        if (ReflectionTestUtils.getField(entity, "id") == null) {
            ReflectionTestUtils.setField(entity, "id", entity instanceof GroupConversation ? GROUP_ID : UUID.randomUUID());
        }
        if (entity instanceof GroupConversation conversation) {
            ReflectionTestUtils.setField(conversation, "createdAt", NOW);
            ReflectionTestUtils.setField(conversation, "updatedAt", NOW);
        } else if (entity instanceof GroupInvitation invitation) {
            ReflectionTestUtils.setField(invitation, "createdAt", NOW);
        } else if (entity instanceof GroupParticipant participant) {
            ReflectionTestUtils.setField(participant, "joinedAt", NOW);
        } else if (entity instanceof GroupMessage message) {
            ReflectionTestUtils.setField(message, "createdAt", NOW);
        }
        return entity;
    }
}
