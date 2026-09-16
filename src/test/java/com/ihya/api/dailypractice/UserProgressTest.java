package com.ihya.api.dailypractice;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit tests for {@link UserProgress#applyPractice}: the strict-reset
 * streak state machine (docs/api-contract.md §2 "Streak"). No repository, no
 * Spring context — this is plain object state, tested directly.
 */
class UserProgressTest {

    @Test
    void applyPractice_firstEverPractice_startsStreakAtOne() {
        UserProgress progress = new UserProgress(UUID.randomUUID());
        LocalDate today = LocalDate.of(2026, 9, 16);

        progress.applyPractice(today);

        assertThat(progress.getStreak()).isEqualTo(1);
        assertThat(progress.getLongestStreak()).isEqualTo(1);
        assertThat(progress.getTotalPracticed()).isEqualTo(1);
        assertThat(progress.getLastPracticeDate()).isEqualTo(today);
    }

    @Test
    void applyPractice_consecutiveLocalDay_continuesStreak() {
        UserProgress progress = new UserProgress(UUID.randomUUID());
        LocalDate day1 = LocalDate.of(2026, 9, 14);
        LocalDate day2 = LocalDate.of(2026, 9, 15);
        LocalDate day3 = LocalDate.of(2026, 9, 16);

        progress.applyPractice(day1);
        progress.applyPractice(day2);
        progress.applyPractice(day3);

        assertThat(progress.getStreak()).isEqualTo(3);
        assertThat(progress.getLongestStreak()).isEqualTo(3);
        assertThat(progress.getTotalPracticed()).isEqualTo(3);
    }

    @Test
    void applyPractice_gapOfOneOrMoreDays_strictlyResetsStreakToOne() {
        UserProgress progress = new UserProgress(UUID.randomUUID());
        progress.applyPractice(LocalDate.of(2026, 9, 10));
        progress.applyPractice(LocalDate.of(2026, 9, 11));
        progress.applyPractice(LocalDate.of(2026, 9, 12)); // streak = 3

        progress.applyPractice(LocalDate.of(2026, 9, 14)); // skipped the 13th

        assertThat(progress.getStreak()).isEqualTo(1);
        assertThat(progress.getLongestStreak()).isEqualTo(3); // high-water mark preserved
        assertThat(progress.getTotalPracticed()).isEqualTo(4);
    }

    @Test
    void applyPractice_afterReset_longestStreakNeverDecreases() {
        UserProgress progress = new UserProgress(UUID.randomUUID());
        progress.applyPractice(LocalDate.of(2026, 9, 1));
        progress.applyPractice(LocalDate.of(2026, 9, 2));
        progress.applyPractice(LocalDate.of(2026, 9, 3));
        progress.applyPractice(LocalDate.of(2026, 9, 3).plusDays(2)); // reset to streak 1

        assertThat(progress.getLongestStreak()).isEqualTo(3);
        assertThat(progress.getStreak()).isEqualTo(1);
    }

    @Test
    void applyPractice_newStreakSurpassesOldLongest_longestStreakUpdates() {
        UserProgress progress = new UserProgress(UUID.randomUUID());
        progress.applyPractice(LocalDate.of(2026, 8, 1));
        progress.applyPractice(LocalDate.of(2026, 8, 5)); // gap -> reset, longest stays 1

        progress.applyPractice(LocalDate.of(2026, 8, 6));
        progress.applyPractice(LocalDate.of(2026, 8, 7)); // streak now 3, surpasses old longest of 1

        assertThat(progress.getStreak()).isEqualTo(3);
        assertThat(progress.getLongestStreak()).isEqualTo(3);
        assertThat(progress.getTotalPracticed()).isEqualTo(4);
    }
}
