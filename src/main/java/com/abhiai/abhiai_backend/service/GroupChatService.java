package com.abhiai.abhiai_backend.service;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.abhiai.abhiai_backend.dto.common.PageResponse;
import com.abhiai.abhiai_backend.dto.groupchat.GroupConversationResponse;
import com.abhiai.abhiai_backend.dto.groupchat.GroupInvitationResponse;
import com.abhiai.abhiai_backend.dto.groupchat.GroupMemberResponse;
import com.abhiai.abhiai_backend.dto.groupchat.GroupMessageResponse;
import com.abhiai.abhiai_backend.dto.post.PostAuthorResponse;
import com.abhiai.abhiai_backend.entity.GroupConversation;
import com.abhiai.abhiai_backend.entity.GroupInvitation;
import com.abhiai.abhiai_backend.entity.GroupInvitationStatus;
import com.abhiai.abhiai_backend.entity.GroupMessage;
import com.abhiai.abhiai_backend.entity.GroupParticipant;
import com.abhiai.abhiai_backend.entity.GroupRole;
import com.abhiai.abhiai_backend.entity.User;
import com.abhiai.abhiai_backend.exception.GroupConversationNotFoundException;
import com.abhiai.abhiai_backend.exception.GroupInvitationNotFoundException;
import com.abhiai.abhiai_backend.exception.GroupMessageNotFoundException;
import com.abhiai.abhiai_backend.exception.InvalidGroupActionException;
import com.abhiai.abhiai_backend.exception.UnauthorizedActionException;
import com.abhiai.abhiai_backend.exception.UserNotFoundException;
import com.abhiai.abhiai_backend.repository.GroupConversationRepository;
import com.abhiai.abhiai_backend.repository.GroupInvitationRepository;
import com.abhiai.abhiai_backend.repository.GroupMessageRepository;
import com.abhiai.abhiai_backend.repository.GroupParticipantRepository;
import com.abhiai.abhiai_backend.repository.UserRepository;

@Service
public class GroupChatService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_MESSAGE_LENGTH = 2000;
    private static final int MAX_GROUP_NAME_LENGTH = 100;

    private final GroupConversationRepository conversationRepository;
    private final GroupParticipantRepository participantRepository;
    private final GroupInvitationRepository invitationRepository;
    private final GroupMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    @Autowired
    public GroupChatService(
            GroupConversationRepository conversationRepository,
            GroupParticipantRepository participantRepository,
            GroupInvitationRepository invitationRepository,
            GroupMessageRepository messageRepository,
            UserRepository userRepository) {
        this(conversationRepository, participantRepository, invitationRepository,
                messageRepository, userRepository, Clock.systemUTC());
    }

    GroupChatService(
            GroupConversationRepository conversationRepository,
            GroupParticipantRepository participantRepository,
            GroupInvitationRepository invitationRepository,
            GroupMessageRepository messageRepository,
            UserRepository userRepository,
            Clock clock) {
        this.conversationRepository = conversationRepository;
        this.participantRepository = participantRepository;
        this.invitationRepository = invitationRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Transactional
    public GroupConversationResponse createGroup(UUID actingUserId, String name, String imageUrl) {
        User owner = requireUser(actingUserId);
        GroupConversation group = conversationRepository.saveAndFlush(
                new GroupConversation(normalizeName(name), normalizeImageUrl(imageUrl, true), owner));
        return toConversationResponse(group, actingUserId);
    }

    @Transactional(readOnly = true)
    public List<GroupConversationResponse> getGroups(UUID actingUserId) {
        requireUser(actingUserId);
        return conversationRepository.findAllForUser(actingUserId).stream()
                .map(group -> toConversationResponse(group, actingUserId))
                .toList();
    }

    @Transactional(readOnly = true)
    public GroupConversationResponse getGroup(UUID actingUserId, UUID groupId) {
        return toConversationResponse(requireAccessible(groupId, actingUserId), actingUserId);
    }

    @Transactional
    public GroupConversationResponse updateGroup(
            UUID actingUserId,
            UUID groupId,
            String name,
            String imageUrl) {
        GroupConversation group = requireAccessible(groupId, actingUserId);
        GroupParticipant actor = requireParticipant(groupId, actingUserId);
        requireManager(actor);
        if (name == null && imageUrl == null) {
            throw new InvalidGroupActionException("Provide a group name or image URL to update");
        }
        group.updateDetails(name == null ? null : normalizeName(name),
                normalizeImageUrl(imageUrl, imageUrl != null), imageUrl != null);
        return toConversationResponse(group, actingUserId);
    }

    @Transactional
    public GroupInvitationResponse inviteMember(UUID actingUserId, UUID groupId, String username) {
        GroupConversation group = requireAccessible(groupId, actingUserId);
        GroupParticipant actor = requireParticipant(groupId, actingUserId);
        requireManager(actor);
        User invitee = userRepository.findByUsernameIgnoreCase(normalizeUsername(username))
                .orElseThrow(UserNotFoundException::new);
        if (invitee.getId().equals(actingUserId)) {
            throw new InvalidGroupActionException("You are already a member of this group");
        }
        if (participantRepository.existsByConversationIdAndUserId(groupId, invitee.getId())) {
            throw new InvalidGroupActionException("This user is already a group member");
        }

        GroupInvitation invitation = invitationRepository
                .findByConversationIdAndInviteeId(groupId, invitee.getId())
                .map(existing -> {
                    if (existing.getStatus() == GroupInvitationStatus.PENDING) {
                        throw new InvalidGroupActionException("This user already has a pending invitation");
                    }
                    existing.renew(actor.getUser());
                    return existing;
                })
                .orElseGet(() -> new GroupInvitation(group, actor.getUser(), invitee));
        group.touch();
        return GroupInvitationResponse.from(invitationRepository.saveAndFlush(invitation));
    }

    @Transactional(readOnly = true)
    public List<GroupInvitationResponse> getPendingInvitations(UUID actingUserId) {
        requireUser(actingUserId);
        return invitationRepository
                .findAllByInviteeIdAndStatusOrderByCreatedAtDesc(
                        actingUserId, GroupInvitationStatus.PENDING)
                .stream()
                .map(GroupInvitationResponse::from)
                .toList();
    }

    @Transactional
    public GroupConversationResponse acceptInvitation(UUID actingUserId, UUID invitationId) {
        GroupInvitation invitation = requirePendingInvitation(invitationId, actingUserId);
        GroupConversation group = invitation.getConversation();
        if (!participantRepository.existsByConversationIdAndUserId(group.getId(), actingUserId)) {
            group.addParticipant(invitation.getInvitee(), GroupRole.MEMBER);
        }
        invitation.respond(GroupInvitationStatus.ACCEPTED, clock.instant());
        group.touch();
        conversationRepository.flush();
        return toConversationResponse(group, actingUserId);
    }

    @Transactional
    public GroupInvitationResponse declineInvitation(UUID actingUserId, UUID invitationId) {
        GroupInvitation invitation = requirePendingInvitation(invitationId, actingUserId);
        invitation.respond(GroupInvitationStatus.DECLINED, clock.instant());
        return GroupInvitationResponse.from(invitation);
    }

    @Transactional
    public GroupConversationResponse updateMemberRole(
            UUID actingUserId,
            UUID groupId,
            UUID memberId,
            GroupRole nextRole) {
        GroupConversation group = requireAccessible(groupId, actingUserId);
        GroupParticipant actor = requireParticipant(groupId, actingUserId);
        if (actor.getRole() != GroupRole.OWNER) {
            throw new UnauthorizedActionException("Only the group owner can change member roles");
        }
        GroupParticipant target = requireParticipant(groupId, memberId);
        if (target.getUser().getId().equals(actingUserId)) {
            throw new InvalidGroupActionException(
                    "Transfer ownership to another member before changing your own role");
        }
        if (nextRole == GroupRole.OWNER) {
            actor.changeRole(GroupRole.ADMIN);
            target.changeRole(GroupRole.OWNER);
            group.transferOwnership(target.getUser());
        } else {
            target.changeRole(nextRole);
            group.touch();
        }
        conversationRepository.flush();
        return toConversationResponse(group, actingUserId);
    }

    @Transactional
    public void removeMember(UUID actingUserId, UUID groupId, UUID memberId) {
        GroupConversation group = requireAccessible(groupId, actingUserId);
        GroupParticipant actor = requireParticipant(groupId, actingUserId);
        requireManager(actor);
        if (actingUserId.equals(memberId)) {
            throw new InvalidGroupActionException("Use the leave group action to remove yourself");
        }
        GroupParticipant target = requireParticipant(groupId, memberId);
        if (target.getRole() == GroupRole.OWNER
                || actor.getRole() == GroupRole.ADMIN && target.getRole() == GroupRole.ADMIN) {
            throw new UnauthorizedActionException("You cannot remove this group member");
        }
        group.removeParticipant(target);
    }

    @Transactional
    public void leaveGroup(UUID actingUserId, UUID groupId) {
        GroupConversation group = requireAccessible(groupId, actingUserId);
        GroupParticipant actor = requireParticipant(groupId, actingUserId);
        if (actor.getRole() == GroupRole.OWNER) {
            if (participantRepository.countByConversationId(groupId) == 1) {
                conversationRepository.delete(group);
                return;
            }
            throw new InvalidGroupActionException(
                    "Transfer ownership to another member before leaving the group");
        }
        group.removeParticipant(actor);
    }

    @Transactional(readOnly = true)
    public PageResponse<GroupMessageResponse> getHistory(
            UUID actingUserId,
            UUID groupId,
            Pageable pageable) {
        requireAccessible(groupId, actingUserId);
        Page<GroupMessage> messages = messageRepository
                .findAllByConversationIdOrderByCreatedAtDescIdDesc(groupId, normalize(pageable));
        return PageResponse.from(messages, GroupMessageResponse::from);
    }

    @Transactional
    public GroupMessageResponse sendMessage(UUID actingUserId, UUID groupId, String content) {
        GroupConversation group = requireAccessible(groupId, actingUserId);
        User sender = requireParticipant(groupId, actingUserId).getUser();
        GroupMessage message = messageRepository.saveAndFlush(
                new GroupMessage(group, sender, normalizeContent(content)));
        group.touch();
        return GroupMessageResponse.from(message);
    }

    @Transactional
    public GroupMessageResponse deleteMessage(
            UUID actingUserId,
            UUID groupId,
            UUID messageId) {
        GroupConversation group = requireAccessible(groupId, actingUserId);
        GroupParticipant actor = requireParticipant(groupId, actingUserId);
        GroupMessage message = messageRepository.findByIdAndConversationId(messageId, groupId)
                .orElseThrow(GroupMessageNotFoundException::new);
        boolean sender = message.getSender().getId().equals(actingUserId);
        if (!sender && !actor.getRole().canManageMembers()) {
            throw new UnauthorizedActionException(
                    "Only the sender or a group administrator can delete this message");
        }
        if (!message.isDeleted()) {
            message.softDelete();
            group.touch();
        }
        return GroupMessageResponse.from(message);
    }

    private GroupConversationResponse toConversationResponse(GroupConversation group, UUID userId) {
        List<GroupMemberResponse> members = group.getParticipants().stream()
                .sorted(Comparator.comparingInt(member -> member.getRole().ordinal()))
                .map(GroupMemberResponse::from)
                .toList();
        GroupRole currentRole = group.getParticipants().stream()
                .filter(member -> member.getUser().getId().equals(userId))
                .map(GroupParticipant::getRole)
                .findFirst()
                .orElseThrow(GroupConversationNotFoundException::new);
        GroupMessage latest = messageRepository
                .findFirstByConversationIdOrderByCreatedAtDescIdDesc(group.getId())
                .orElse(null);
        return new GroupConversationResponse(
                group.getId(),
                group.getName(),
                group.getImageUrl(),
                PostAuthorResponse.from(group.getOwner()),
                members,
                members.size(),
                currentRole,
                latest == null ? null : latest.isDeleted() ? "Message deleted" : preview(latest.getContent()),
                latest == null ? null : latest.getCreatedAt(),
                group.getCreatedAt(),
                group.getUpdatedAt());
    }

    private GroupConversation requireAccessible(UUID groupId, UUID userId) {
        return conversationRepository.findAccessible(groupId, userId)
                .orElseThrow(GroupConversationNotFoundException::new);
    }

    private GroupParticipant requireParticipant(UUID groupId, UUID userId) {
        return participantRepository.findByConversationIdAndUserId(groupId, userId)
                .orElseThrow(GroupConversationNotFoundException::new);
    }

    private GroupInvitation requirePendingInvitation(UUID invitationId, UUID userId) {
        GroupInvitation invitation = invitationRepository.findByIdAndInviteeId(invitationId, userId)
                .orElseThrow(GroupInvitationNotFoundException::new);
        if (invitation.getStatus() != GroupInvitationStatus.PENDING) {
            throw new InvalidGroupActionException("This group invitation is no longer pending");
        }
        return invitation;
    }

    private User requireUser(UUID userId) {
        return userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
    }

    private void requireManager(GroupParticipant participant) {
        if (!participant.getRole().canManageMembers()) {
            throw new UnauthorizedActionException("Group administrator permission is required");
        }
    }

    private String normalizeName(String name) {
        String normalized = name == null ? "" : name.trim().replaceAll("\\s+", " ");
        if (normalized.isEmpty()) throw new InvalidGroupActionException("Group name is required");
        if (normalized.codePointCount(0, normalized.length()) > MAX_GROUP_NAME_LENGTH) {
            throw new InvalidGroupActionException(
                    "Group name must not exceed " + MAX_GROUP_NAME_LENGTH + " characters");
        }
        return normalized;
    }

    private String normalizeUsername(String username) {
        String normalized = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("@")) normalized = normalized.substring(1);
        if (normalized.isBlank()) throw new InvalidGroupActionException("Username is required");
        return normalized;
    }

    private String normalizeImageUrl(String imageUrl, boolean updateImage) {
        if (!updateImage) return null;
        String normalized = imageUrl == null ? null : imageUrl.trim();
        if (normalized == null || normalized.isEmpty()) return null;
        try {
            URI uri = URI.create(normalized);
            if (!("http".equalsIgnoreCase(uri.getScheme())
                    || "https".equalsIgnoreCase(uri.getScheme())) || uri.getHost() == null) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException exception) {
            throw new InvalidGroupActionException("Group image must be a valid HTTP or HTTPS URL");
        }
        return normalized;
    }

    private String normalizeContent(String content) {
        String normalized = content == null ? "" : content.trim();
        if (normalized.isEmpty()) throw new InvalidGroupActionException("Message content is required");
        if (normalized.codePointCount(0, normalized.length()) > MAX_MESSAGE_LENGTH) {
            throw new InvalidGroupActionException(
                    "Message content must not exceed " + MAX_MESSAGE_LENGTH + " characters");
        }
        return normalized;
    }

    private String preview(String content) {
        String normalized = content.replaceAll("\\s+", " ");
        return normalized.length() <= 90 ? normalized : normalized.substring(0, 89).trim() + "…";
    }

    private Pageable normalize(Pageable pageable) {
        int page = Math.max(0, pageable.getPageNumber());
        int size = Math.max(1, Math.min(pageable.getPageSize(), MAX_PAGE_SIZE));
        return PageRequest.of(page, size);
    }
}
