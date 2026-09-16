package com.ihya.api.notification;

import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.UUID;

@Service
public class NotificationPreferencesService {

    private final NotificationPreferencesRepository notificationPreferencesRepository;

    public NotificationPreferencesService(NotificationPreferencesRepository notificationPreferencesRepository) {
        this.notificationPreferencesRepository = notificationPreferencesRepository;
    }

    public void createDefaults(UUID userId) {
        notificationPreferencesRepository.save(new NotificationPreferences(userId));
    }

    /**
     * A preferences row is guaranteed to exist for every user — {@link com.ihya.api.identity.UserService#register}
     * creates it in the same transaction as the user row, with no lazy-create path.
     * A miss here means that invariant was violated, so it's an unchecked failure
     * (logged 500 via the shared catch-all), not a typed domain exception. Mirrors
     * {@code ProfileServiceImpl#getProfile}.
     */
    public NotificationPreferences getPreferences(UUID userId) {
        return notificationPreferencesRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("No notification_preferences row for user " + userId));
    }

    public NotificationPreferences updatePreferences(UUID userId, Boolean dailyReminder, Boolean streakReminder,
                                                       Boolean weeklySummary, LocalTime reminderTime) {
        NotificationPreferences preferences = getPreferences(userId);
        if (dailyReminder != null) {
            preferences.setDailyReminder(dailyReminder);
        }
        if (streakReminder != null) {
            preferences.setStreakReminder(streakReminder);
        }
        if (weeklySummary != null) {
            preferences.setWeeklySummary(weeklySummary);
        }
        if (reminderTime != null) {
            preferences.setReminderTime(reminderTime);
        }
        return notificationPreferencesRepository.save(preferences);
    }

    public void deleteForUser(UUID userId) {
        notificationPreferencesRepository.deleteById(userId);
    }
}
