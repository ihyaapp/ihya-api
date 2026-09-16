package com.ihya.api.dailypractice;

/**
 * Result of a successful {@link AssignmentService#getTodayAssignment} or
 * {@link AssignmentService#requestReplacement} call: the resolved
 * {@link DailyAssignment} for today, paired with whether a replacement is
 * still available ({@code !replacementUsed && !practicedToday} — see
 * docs/api-contract.md §1 "Assignment").
 */
public record AssignmentResult(DailyAssignment assignment, boolean replacementAvailable) {
}
