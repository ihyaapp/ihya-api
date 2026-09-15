package com.ihya.api.catalogue;

/**
 * A category with the given slug already exists.
 *
 * <p>Same persist-and-remap technique as {@link CategoryNameAlreadyExistsException},
 * against the {@code categories_slug_key} unique constraint (V9). Kept as its
 * own type rather than folded into the name exception, so the 409 message
 * names the field that actually collided. Maps to <strong>409</strong> at the
 * controller layer.
 */
public class CategorySlugAlreadyExistsException extends RuntimeException {
    public CategorySlugAlreadyExistsException(String slug) {
        super("Category slug already exists: " + slug);
    }
}
