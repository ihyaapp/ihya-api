package com.ihya.api.dailypractice;

import com.ihya.api.catalogue.SunnahResponse;

/**
 * Client-facing shape for {@code POST /assignment/replacement}
 * (docs/api-contract.md §2) — deliberately just {@code { sunnah }}, not the
 * full {@link AssignmentResponse} shape {@code GET /assignment/today} returns.
 */
public record ReplacementResponse(SunnahResponse sunnah) {
}
