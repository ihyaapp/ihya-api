package com.ihya.api.dailypractice;

import java.util.List;

/**
 * Client-facing shape for {@code GET /me/progress} (docs/api-contract.md §1
 * "Progress"). {@code earnedMilestoneKeys} — see {@link MilestoneEvaluator}
 * for why the keys are placeholders.
 */
public record ProgressResponse(int streak, int longestStreak, int totalPracticed, List<String> earnedMilestoneKeys) {

    public static ProgressResponse from(UserProgress progress) {
        return new ProgressResponse(
                progress.getStreak(),
                progress.getLongestStreak(),
                progress.getTotalPracticed(),
                MilestoneEvaluator.allEarned(progress.getLongestStreak(), progress.getTotalPracticed()));
    }
}
