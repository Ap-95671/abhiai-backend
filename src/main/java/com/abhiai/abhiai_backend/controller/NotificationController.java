package com.abhiai.abhiai_backend.controller;

import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import com.abhiai.abhiai_backend.dto.common.PageResponse;
import com.abhiai.abhiai_backend.dto.notification.MarkAllNotificationsReadResponse;
import com.abhiai.abhiai_backend.dto.notification.NotificationResponse;
import com.abhiai.abhiai_backend.dto.notification.UnreadNotificationCountResponse;
import com.abhiai.abhiai_backend.security.JwtPrincipal;
import com.abhiai.abhiai_backend.service.SocialNotificationService;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final SocialNotificationService notificationService;

    public NotificationController(SocialNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<NotificationResponse>> getNotifications(
            @AuthenticationPrincipal JwtPrincipal principal,
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(
                notificationService.getNotifications(principal.userId(), unreadOnly, pageable));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<UnreadNotificationCountResponse> getUnreadCount(
            @AuthenticationPrincipal JwtPrincipal principal) {
        return ResponseEntity.ok(notificationService.getUnreadCount(principal.userId()));
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<NotificationResponse> markRead(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID notificationId) {
        return ResponseEntity.ok(
                notificationService.markRead(principal.userId(), notificationId));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<MarkAllNotificationsReadResponse> markAllRead(
            @AuthenticationPrincipal JwtPrincipal principal) {
        return ResponseEntity.ok(notificationService.markAllRead(principal.userId()));
    }
}
