package com.ihya.api.notification;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Storage for how a user wants to be reminded, and where to push to
 * (docs/api-contract.md §2 "Me & preferences"). Reads = the caller's own row,
 * guaranteed to exist by {@link NotificationPreferencesService} — see its
 * {@code getPreferences} javadoc.
 *
 * <p><strong>Storage only</strong> (Phase 4): nothing yet reads these rows to
 * actually send a reminder. That scheduler + Expo push call is deliberately
 * deferred scope — see the roadmap artifact and docs/api-contract.md §5.
 */
@RestController
public class NotificationController {

    private static final DateTimeFormatter REMINDER_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final NotificationPreferencesService notificationPreferencesService;
    private final PushTokenService pushTokenService;

    public NotificationController(NotificationPreferencesService notificationPreferencesService,
                                   PushTokenService pushTokenService) {
        this.notificationPreferencesService = notificationPreferencesService;
        this.pushTokenService = pushTokenService;
    }

    @GetMapping("/me/notification-preferences")
    public NotificationPreferencesResponse getPreferences(@AuthenticationPrincipal UUID userId) {
        return NotificationPreferencesResponse.from(notificationPreferencesService.getPreferences(userId));
    }

    @PatchMapping("/me/notification-preferences")
    public NotificationPreferencesResponse updatePreferences(@AuthenticationPrincipal UUID userId,
            @Valid @RequestBody UpdateNotificationPreferencesRequest request) {
        LocalTime reminderTime = request.reminderTime() == null
                ? null
                : LocalTime.parse(request.reminderTime(), REMINDER_TIME_FORMAT);
        NotificationPreferences updated = notificationPreferencesService.updatePreferences(
                userId, request.dailyReminder(), request.streakReminder(), request.weeklySummary(), reminderTime);
        return NotificationPreferencesResponse.from(updated);
    }

    @PostMapping("/me/push-tokens")
    public ResponseEntity<Void> registerPushToken(@AuthenticationPrincipal UUID userId,
            @Valid @RequestBody PushTokenRequest request) {
        pushTokenService.registerToken(userId, request.expoPushToken(), request.platform());
        return ResponseEntity.noContent().build();
    }
}
