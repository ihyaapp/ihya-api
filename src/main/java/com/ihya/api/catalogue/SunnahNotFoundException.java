package com.ihya.api.catalogue;

import java.util.UUID;

/**
 * No {@code sunnahs} row exists for the given id or slug.
 *
 * <p>Same convention as {@link CategoryNotFoundException}: dedicated
 * per-resource exception, mapped to <strong>404</strong> by
 * {@code CatalogueExceptionHandler}.
 */
public class SunnahNotFoundException extends RuntimeException {
    public SunnahNotFoundException(UUID id) {
        super("Sunnah not found: " + id);
    }

    public SunnahNotFoundException(String slug) {
        super("Sunnah not found: " + slug);
    }
}
