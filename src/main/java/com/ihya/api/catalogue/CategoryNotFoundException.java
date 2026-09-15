package com.ihya.api.catalogue;

import java.util.UUID;

/**
 * No {@code categories} row exists for the given id or slug.
 *
 * <p>Follows the identity module's convention of a dedicated per-resource
 * exception ({@code UserNotFoundException}); a plain missing-resource error,
 * mapped to <strong>404</strong> by {@code CatalogueExceptionHandler}.
 */
public class CategoryNotFoundException extends RuntimeException {
    public CategoryNotFoundException(UUID id) {
        super("Category not found: " + id);
    }

    public CategoryNotFoundException(String slug) {
        super("Category not found: " + slug);
    }
}
