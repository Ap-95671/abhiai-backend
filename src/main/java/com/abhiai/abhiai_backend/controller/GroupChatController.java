package com.abhiai.abhiai_backend.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.abhiai.abhiai_backend.dto.common.PageResponse;
import com.abhiai.abhiai_backend.dto.groupchat.CreateGroupRequest;
import com.abhiai.abhiai_backend.dto.groupchat.GroupConversationResponse;
import com.abhiai.abhiai_backend.dto.groupchat.GroupInvitationResponse;
import com.abhiai.abhiai_backend.dto.groupchat.GroupMessageResponse;
import com.abhiai.abhiai_backend.dto.groupchat.InviteGroupMemberRequest;
import com.abhiai.abhiai_backend.dto.groupchat.SendGroupMessageRequest;
import com.abhiai.abhiai_backend.dto.groupchat.UpdateGroupMemberRoleRequest;
import com.abhiai.abhiai_backend.dto.groupchat.UpdateGroupRequest;
import com.abhiai.abhiai_backend.security.JwtPrincipal;
import com.abhiai.abhiai_backend.service.GroupChatService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/group-chats")
public class GroupChatController {

    private final GroupChatService groupChatService;

    public GroupChatController(GroupChatService groupChatService) {
        this.groupChatService = groupChatService;
    }

    @PostMapping
    public ResponseEntity<GroupConversationResponse> createGroup(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody CreateGroupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                groupChatService.createGroup(principal.userId(), request.name(), request.imageUrl()));
    }

    @GetMapping
    public ResponseEntity<List<GroupConversationResponse>> getGroups(
            @AuthenticationPrincipal JwtPrincipal principal) {
        return ResponseEntity.ok(groupChatService.getGroups(principal.userId()));
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<GroupConversationResponse> getGroup(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID groupId) {
        return ResponseEntity.ok(groupChatService.getGroup(principal.userId(), groupId));
    }

    @PatchMapping("/{groupId}")
    public ResponseEntity<GroupConversationResponse> updateGroup(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID groupId,
            @Valid @RequestBody UpdateGroupRequest request) {
        return ResponseEntity.ok(groupChatService.updateGroup(
                principal.userId(), groupId, request.name(), request.imageUrl()));
    }

    @PostMapping("/{groupId}/invitations")
    public ResponseEntity<GroupInvitationResponse> inviteMember(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID groupId,
            @Valid @RequestBody InviteGroupMemberRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                groupChatService.inviteMember(principal.userId(), groupId, request.username()));
    }

    @GetMapping("/invitations/pending")
    public ResponseEntity<List<GroupInvitationResponse>> getPendingInvitations(
            @AuthenticationPrincipal JwtPrincipal principal) {
        return ResponseEntity.ok(groupChatService.getPendingInvitations(principal.userId()));
    }

    @PatchMapping("/invitations/{invitationId}/accept")
    public ResponseEntity<GroupConversationResponse> acceptInvitation(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID invitationId) {
        return ResponseEntity.ok(
                groupChatService.acceptInvitation(principal.userId(), invitationId));
    }

    @PatchMapping("/invitations/{invitationId}/decline")
    public ResponseEntity<GroupInvitationResponse> declineInvitation(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID invitationId) {
        return ResponseEntity.ok(
                groupChatService.declineInvitation(principal.userId(), invitationId));
    }

    @PatchMapping("/{groupId}/members/{memberId}/role")
    public ResponseEntity<GroupConversationResponse> updateMemberRole(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID groupId,
            @PathVariable UUID memberId,
            @Valid @RequestBody UpdateGroupMemberRoleRequest request) {
        return ResponseEntity.ok(groupChatService.updateMemberRole(
                principal.userId(), groupId, memberId, request.role()));
    }

    @DeleteMapping("/{groupId}/members/{memberId}")
    public ResponseEntity<Void> removeMember(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID groupId,
            @PathVariable UUID memberId) {
        groupChatService.removeMember(principal.userId(), groupId, memberId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{groupId}/membership")
    public ResponseEntity<Void> leaveGroup(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID groupId) {
        groupChatService.leaveGroup(principal.userId(), groupId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{groupId}/messages")
    public ResponseEntity<PageResponse<GroupMessageResponse>> getHistory(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID groupId,
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(
                groupChatService.getHistory(principal.userId(), groupId, pageable));
    }

    @PostMapping("/{groupId}/messages")
    public ResponseEntity<GroupMessageResponse> sendMessage(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID groupId,
            @Valid @RequestBody SendGroupMessageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                groupChatService.sendMessage(principal.userId(), groupId, request.content()));
    }

    @DeleteMapping("/{groupId}/messages/{messageId}")
    public ResponseEntity<GroupMessageResponse> deleteMessage(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID groupId,
            @PathVariable UUID messageId) {
        return ResponseEntity.ok(
                groupChatService.deleteMessage(principal.userId(), groupId, messageId));
    }
}
