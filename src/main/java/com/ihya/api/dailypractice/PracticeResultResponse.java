package com.ihya.api.dailypractice;

/**
 * Client-facing shape for {@code POST /practices}
 * (docs/api-contract.md §1 "PracticeResult"). Used for both the {@code 201}
 * (newly recorded) and {@code 409} (already practiced today) responses — the
 * controller picks the status code from {@link PracticeRecordResult#alreadyExisted()};
 * the body shape is identical either way, only {@code milestoneUnlocked} is
 * always {@code null} on the {@code 409} path.
 */
public record PracticeResultResponse(
        PracticeResponse practice, int streak, int longestStreak, int totalPracticed, String milestoneUnlocked) {

    public static PracticeResultResponse from(PracticeRecordResult result) {
        return new PracticeResultResponse(
                PracticeResponse.from(result.practice()),
                result.progress().getStreak(),
                result.progress().getLongestStreak(),
                result.progress().getTotalPracticed(),
                result.milestoneUnlocked());
    }
}
