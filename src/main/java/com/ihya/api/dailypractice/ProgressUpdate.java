package com.ihya.api.dailypractice;

/**
 * Result of a successful {@link UserProgressService#recordPractice} call: the
 * updated {@link UserProgress} row, paired with its pre-update
 * {@code longestStreak}/{@code totalPracticed}. {@link PracticeService} needs
 * the "before" values to tell whether this write newly crossed a milestone
 * threshold, not just the "after" state.
 */
public record ProgressUpdate(UserProgress progress, int previousLongestStreak, int previousTotalPracticed) {
}
