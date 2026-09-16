package com.ihya.api.dailypractice;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "user_progress")
public class UserProgress {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "streak", nullable = false)
    private int streak;

    @Column(name = "longest_streak", nullable = false)
    private int longestStreak;

    @Column(name = "total_practiced", nullable = false)
    private int totalPracticed;

    @Column(name = "last_practice_date")
    private LocalDate lastPracticeDate;

    protected UserProgress() {
        // required by Hibernate
    }

    public UserProgress(UUID userId) {
        this.userId = userId;
        this.streak = 0;
        this.longestStreak = 0;
        this.totalPracticed = 0;
        this.lastPracticeDate = null;
    }

    /**
     * Applies a newly-recorded practice for {@code practiceDate} (locked
     * decision, docs/api-contract.md §2 "Streak"): strict reset. Continues the
     * streak only when the previous practice was exactly the local day before;
     * any earlier gap resets to 1. Only ever called once per day per user --
     * the caller only reaches this after winning the
     * {@code UNIQUE(user_id, practice_date)} race on the practice insert -- so
     * there's no same-day-twice case to guard against here.
     */
    public void applyPractice(LocalDate practiceDate) {
        if (lastPracticeDate != null && lastPracticeDate.equals(practiceDate.minusDays(1))) {
            streak = streak + 1;
        } else {
            streak = 1;
        }
        longestStreak = Math.max(longestStreak, streak);
        totalPracticed = totalPracticed + 1;
        lastPracticeDate = practiceDate;
    }

    public UUID getUserId() {
        return userId;
    }

    public int getStreak() {
        return streak;
    }

    public int getLongestStreak() {
        return longestStreak;
    }

    public int getTotalPracticed() {
        return totalPracticed;
    }

    public LocalDate getLastPracticeDate() {
        return lastPracticeDate;
    }
}
