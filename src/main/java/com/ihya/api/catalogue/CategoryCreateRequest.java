package com.ihya.api.catalogue;

import jakarta.validation.constraints.NotBlank;

/**
 * Incoming payload for {@code POST /categories} (ADMIN only).
 *
 * <p>{@code status} is optional — a create with it omitted defaults to
 * {@code "active"} in {@link CategoryService}; when present it is validated
 * against the same {@code "active"} / {@code "coming-soon"} set the
 * {@code categories_status} check constraint (V9) enforces at the DB level.
 */
public record CategoryCreateRequest(
        @NotBlank String slug,
        @NotBlank String name,
        String description,
        String status) {
}
