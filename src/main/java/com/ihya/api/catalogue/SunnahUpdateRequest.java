package com.ihya.api.catalogue;

import java.util.List;

/**
 * Incoming payload for {@code PATCH /sunnahs/{slug}} (ADMIN only).
 *
 * <p>Every field is optional — {@code null} means "leave unchanged" — same
 * partial-update convention as {@code UpdateMeRequest}. {@code slug} is
 * deliberately not patchable here, for the same "never re-slug" reason as
 * {@link CategoryUpdateRequest}. {@code tags}, when present, replaces the
 * whole list rather than merging into it.
 */
public record SunnahUpdateRequest(
        String title,
        String categorySlug,
        String source,
        String description,
        String reflection,
        String arabicText,
        String prompt,
        List<String> tags) {
}
