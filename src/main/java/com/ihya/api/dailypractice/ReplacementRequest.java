package com.ihya.api.dailypractice;

import jakarta.validation.constraints.NotBlank;

/**
 * Incoming payload for {@code POST /assignment/replacement}. {@code reason} is
 * stored for analytics only — it never affects which Sunnah is picked
 * (docs/api-contract.md §2 "Selection logic").
 */
public record ReplacementRequest(@NotBlank String reason) {
}
