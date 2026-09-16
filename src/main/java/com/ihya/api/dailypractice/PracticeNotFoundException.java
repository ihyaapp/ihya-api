package com.ihya.api.dailypractice;

import java.util.UUID;

/**
 * No {@code practices} row exists for the given id, owned by the caller. A
 * practice belonging to a different user is deliberately reported the same
 * way — not a {@code 403} — so a request can't use this endpoint to probe
 * which practice ids exist for other users. Mapped to {@code 404} by
 * {@code DailyPracticeExceptionHandler}.
 */
public class PracticeNotFoundException extends RuntimeException {
    public PracticeNotFoundException(UUID id) {
        super("Practice not found: " + id);
    }
}
