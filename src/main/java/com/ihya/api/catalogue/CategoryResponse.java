package com.ihya.api.catalogue;

/**
 * Public view of a {@link Category}, returned by every catalogue read.
 *
 * <p>{@code sunnahCount} is derived at query time (never authored — see
 * ihya-mobile/docs/content-model.md), so it is passed in separately rather
 * than read off the entity, which has no such field.
 */
public record CategoryResponse(String slug, String name, String description, String status, long sunnahCount) {

    static CategoryResponse from(Category category, long sunnahCount) {
        return new CategoryResponse(
                category.getSlug(),
                category.getName(),
                category.getDescription(),
                category.getStatus(),
                sunnahCount);
    }
}
