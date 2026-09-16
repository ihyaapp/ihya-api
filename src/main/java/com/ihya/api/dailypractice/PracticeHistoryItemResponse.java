package com.ihya.api.dailypractice;

import com.ihya.api.catalogue.SunnahResponse;

import java.util.UUID;

/**
 * One item of {@code GET /practices} — {@code Practice & { sunnah: Sunnah }}
 * (docs/api-contract.md §2 "Daily practice"): every {@link Practice} field,
 * flattened, plus the full {@link SunnahResponse} it was practiced for.
 */
public record PracticeHistoryItemResponse(
        UUID id, UUID sunnahId, String practiceDate, String feeling, SunnahResponse sunnah) {

    public static PracticeHistoryItemResponse from(Practice practice, SunnahResponse sunnah) {
        return new PracticeHistoryItemResponse(
                practice.getId(), practice.getSunnahId(), practice.getPracticeDate().toString(),
                practice.getFeeling(), sunnah);
    }
}
