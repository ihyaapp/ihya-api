package com.ihya.api.catalogue;

/**
 * A Sunnah with the given slug already exists.
 *
 * <p>Same persist-and-remap technique as {@link CategorySlugAlreadyExistsException},
 * against the {@code sunnahs_slug_key} unique constraint (V9). Maps to
 * <strong>409</strong> at the controller layer.
 */
public class SunnahSlugAlreadyExistsException extends RuntimeException {
    public SunnahSlugAlreadyExistsException(String slug) {
        super("Sunnah slug already exists: " + slug);
    }
}
