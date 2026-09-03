package com.ihya.api.catalogue;

import java.util.UUID;

/**
 * No {@code categories} row exists for the given id.
 *
 * <p>Follows the identity module's convention of a dedicated per-resource
 * exception ({@code UserNotFoundException}); unlike that one this is a plain
 * missing-resource error and is expected to map to <strong>404</strong> once the
 * catalogue controller / exception handling is wired (next task).
 */
public class CategoryNotFoundException extends RuntimeException {
    public CategoryNotFoundException(UUID id) {
        super("Category not found: " + id);
    }
}
