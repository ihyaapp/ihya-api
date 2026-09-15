package com.ihya.api.catalogue;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * Incoming payload for {@code POST /sunnahs} (ADMIN only) — the {@code Sunnah}
 * shape "sans id" per docs/api-contract.md §2, keyed to a category by its
 * slug rather than its UUID id.
 */
public record SunnahCreateRequest(
        @NotBlank String slug,
        @NotBlank String title,
        @NotBlank String categorySlug,
        @NotBlank String source,
        @NotBlank String description,
        @NotBlank String reflection,
        String arabicText,
        String prompt,
        List<String> tags) {
}
