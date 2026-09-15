package com.ihya.api.catalogue;

/**
 * Incoming payload for {@code PATCH /categories/{slug}} (ADMIN only).
 *
 * <p>Every field is optional — {@code null} means "leave unchanged" — same
 * partial-update convention as {@code UpdateMeRequest}. {@code slug} is
 * deliberately not patchable here: it is the stable, client-facing key
 * (seed-data-spec.md: "never re-slug an existing" category), so renaming one
 * is not exposed through this endpoint.
 */
public record CategoryUpdateRequest(
        String name,
        String description,
        String status) {
}
