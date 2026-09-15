package com.ihya.api.catalogue;

import java.util.List;
import java.util.UUID;

/**
 * Public view of a {@link Sunnah}, returned by every catalogue read.
 *
 * <p>{@code categorySlug} is read off the associated {@link Category} rather
 * than stored directly on {@code Sunnah} — client payloads reference a
 * category only by its stable slug, never the UUID id
 * (docs/api-contract.md §0 "Category reference").
 */
public record SunnahResponse(
        UUID id,
        String slug,
        String title,
        String categorySlug,
        String source,
        String description,
        String reflection,
        String arabicText,
        String prompt,
        List<String> tags) {

    static SunnahResponse from(Sunnah sunnah) {
        return new SunnahResponse(
                sunnah.getId(),
                sunnah.getSlug(),
                sunnah.getTitle(),
                sunnah.getCategory().getSlug(),
                sunnah.getSource(),
                sunnah.getDescription(),
                sunnah.getReflection(),
                sunnah.getArabicText(),
                sunnah.getPrompt(),
                sunnah.getTags());
    }
}
