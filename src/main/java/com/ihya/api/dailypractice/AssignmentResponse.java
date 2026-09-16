package com.ihya.api.dailypractice;

import com.ihya.api.catalogue.SunnahResponse;

/**
 * Client-facing shape for {@code GET /assignment/today}
 * (docs/api-contract.md §1 "Assignment").
 */
public record AssignmentResponse(SunnahResponse sunnah, boolean replacementAvailable) {
}
