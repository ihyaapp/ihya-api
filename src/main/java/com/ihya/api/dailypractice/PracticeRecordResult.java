package com.ihya.api.dailypractice;

/**
 * Result of a {@link PracticeService#recordPractice} call.
 *
 * <p>{@code alreadyExisted} is {@code true} when the user had already
 * practiced today — the {@code UNIQUE(user_id, practice_date)} write lost the
 * race (or simply found the row already there), so {@code practice} and
 * {@code progress} carry the pre-existing state and {@code milestoneUnlocked}
 * is {@code null}. Deliberately not a thrown exception: docs/api-contract.md
 * §2 wants this case rendered as {@code 409} with the same body shape as a
 * success (existing practice + current progress), not the shared
 * {@code ErrorResponse} shape every other error uses — so the controller
 * layer picks the status code from this flag rather than catching an
 * exception.
 */
public record PracticeRecordResult(Practice practice, UserProgress progress, String milestoneUnlocked,
                                    boolean alreadyExisted) {

    static PracticeRecordResult created(Practice practice, UserProgress progress, String milestoneUnlocked) {
        return new PracticeRecordResult(practice, progress, milestoneUnlocked, false);
    }

    static PracticeRecordResult alreadyExisted(Practice practice, UserProgress progress) {
        return new PracticeRecordResult(practice, progress, null, true);
    }
}
