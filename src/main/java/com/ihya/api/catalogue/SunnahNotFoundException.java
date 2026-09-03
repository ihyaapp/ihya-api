package com.ihya.api.catalogue;

import java.util.UUID;

/**
 * No {@code sunnahs} row exists for the given id.
 *
 * <p>Same convention as {@link CategoryNotFoundException}: dedicated per-resource
 * exception, expected to map to <strong>404</strong> when the catalogue
 * controller layer is added.
 */
public class SunnahNotFoundException extends RuntimeException {
    public SunnahNotFoundException(UUID id) {
        super("Sunnah not found: " + id);
    }
}
