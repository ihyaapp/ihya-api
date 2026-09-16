package com.ihya.api.notification;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * The in-app notification feed (docs/api-contract.md §2 "Notifications").
 * Separate from {@link NotificationController} (which owns preferences/push
 * tokens) the same way {@code AssignmentController}/{@code PracticeController}
 * split within the dailypractice module — one controller per resource within
 * the same module.
 */
@RestController
public class NotificationFeedController {

    private final NotificationService notificationService;

    public NotificationFeedController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/notifications")
    public NotificationFeedPageResponse listNotifications(@AuthenticationPrincipal UUID userId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit) {
        NotificationPage page = notificationService.listNotifications(userId, cursor, limit);
        return new NotificationFeedPageResponse(
                page.items().stream().map(NotificationResponse::from).toList(), page.nextCursor());
    }

    @PostMapping("/notifications/read")
    public ResponseEntity<Void> markRead(@AuthenticationPrincipal UUID userId,
            @RequestBody(required = false) MarkNotificationsReadRequest request) {
        notificationService.markRead(userId, request == null ? null : request.ids());
        return ResponseEntity.noContent().build();
    }
}
