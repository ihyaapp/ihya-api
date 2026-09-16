package com.ihya.api.dailypractice;

import jakarta.validation.constraints.NotBlank;

/**
 * Incoming payload for {@code PATCH /practices/{id}}. Feeling only — this
 * endpoint never re-triggers streak math (docs/api-contract.md §2).
 */
public record UpdateFeelingRequest(@NotBlank String feeling) {
}
