package com.ihya.api.dailypractice;

import java.util.UUID;

/**
 * Client-facing shape for a {@link Practice} (docs/api-contract.md §1
 * "Practice"). {@code practiceDate} is rendered as a plain
 * {@code "YYYY-MM-DD"} string via {@link java.time.LocalDate#toString()}.
 */
public record PracticeResponse(UUID id, UUID sunnahId, String practiceDate, String feeling) {

    public static PracticeResponse from(Practice practice) {
        return new PracticeResponse(
                practice.getId(), practice.getSunnahId(), practice.getPracticeDate().toString(),
                practice.getFeeling());
    }
}
