package com.ihya.api.notification;

import java.time.format.DateTimeFormatter;

/**
 * Client-facing shape for {@code GET}/{@code PATCH /me/notification-preferences}
 * (docs/api-contract.md §1 "NotificationPreferences"). {@code reminderTime} is
 * rendered as a plain {@code "HH:mm"} string — the entity's {@link java.time.LocalTime}
 * carries seconds, which the contract doesn't expose.
 */
public record NotificationPreferencesResponse(
        boolean dailyReminder,
        boolean streakReminder,
        boolean weeklySummary,
        String reminderTime) {

    private static final DateTimeFormatter REMINDER_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    public static NotificationPreferencesResponse from(NotificationPreferences preferences) {
        return new NotificationPreferencesResponse(
                preferences.isDailyReminder(),
                preferences.isStreakReminder(),
                preferences.isWeeklySummary(),
                preferences.getReminderTime().format(REMINDER_TIME_FORMAT));
    }
}
