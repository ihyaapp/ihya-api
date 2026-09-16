package com.ihya.api.notification;

import jakarta.validation.constraints.Pattern;

/**
 * Incoming payload for {@code PATCH /me/notification-preferences}.
 *
 * <p>Every field is optional — {@code null} means "leave unchanged" — same
 * partial-update convention as {@code UpdateMeRequest}. {@link Pattern} only
 * fires on a non-null {@code reminderTime}.
 */
public record UpdateNotificationPreferencesRequest(
        Boolean dailyReminder,
        Boolean streakReminder,
        Boolean weeklySummary,
        @Pattern(regexp = "([01]\\d|2[0-3]):[0-5]\\d", message = "reminderTime must be in HH:mm 24h format")
        String reminderTime) {
}
