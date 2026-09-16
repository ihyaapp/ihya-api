package com.ihya.api.dailypractice;

/**
 * {@code POST /assignment/replacement} was called when a replacement isn't
 * available — either today's assignment already used its one replacement, or
 * the user already practiced today (docs/api-contract.md §2: {@code 409}
 * "already replaced / already practiced today"). Mapped to {@code 409} by
 * {@code DailyPracticeExceptionHandler}.
 */
public class ReplacementNotAvailableException extends RuntimeException {

    private ReplacementNotAvailableException(String message) {
        super(message);
    }

    public static ReplacementNotAvailableException alreadyUsed() {
        return new ReplacementNotAvailableException("Today's assignment has already been replaced once");
    }

    public static ReplacementNotAvailableException alreadyPracticedToday() {
        return new ReplacementNotAvailableException("Today's Sunnah has already been practiced");
    }
}
