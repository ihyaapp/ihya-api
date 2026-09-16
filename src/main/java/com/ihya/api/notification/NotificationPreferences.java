package com.ihya.api.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "notification_preferences")
public class NotificationPreferences {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "daily_reminder", nullable = false)
    private boolean dailyReminder;

    @Column(name = "streak_reminder", nullable = false)
    private boolean streakReminder;

    @Column(name = "weekly_summary", nullable = false)
    private boolean weeklySummary;

    @Column(name = "reminder_time", nullable = false)
    private LocalTime reminderTime;

    protected NotificationPreferences() {
        // required by Hibernate
    }

    public NotificationPreferences(UUID userId) {
        this.userId = userId;
        this.dailyReminder = true;
        this.streakReminder = true;
        this.weeklySummary = false;
        this.reminderTime = LocalTime.of(9, 0);
    }

    public UUID getUserId() {
        return userId;
    }

    public boolean isDailyReminder() {
        return dailyReminder;
    }

    public void setDailyReminder(boolean dailyReminder) {
        this.dailyReminder = dailyReminder;
    }

    public boolean isStreakReminder() {
        return streakReminder;
    }

    public void setStreakReminder(boolean streakReminder) {
        this.streakReminder = streakReminder;
    }

    public boolean isWeeklySummary() {
        return weeklySummary;
    }

    public void setWeeklySummary(boolean weeklySummary) {
        this.weeklySummary = weeklySummary;
    }

    public LocalTime getReminderTime() {
        return reminderTime;
    }

    public void setReminderTime(LocalTime reminderTime) {
        this.reminderTime = reminderTime;
    }
}
