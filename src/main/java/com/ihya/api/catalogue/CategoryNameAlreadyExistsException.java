package com.ihya.api.catalogue;

/**
 * A category with the given name already exists.
 *
 * <p>Mirrors {@code EmailAlreadyRegisteredException} in the identity module: the
 * service does not do a check-then-insert (which would race); it lets the
 * {@code categories_name_key} unique constraint reject the duplicate and remaps
 * that {@code DataIntegrityViolationException} to this. Expected to map to
 * <strong>409</strong> at the controller layer (next task).
 */
public class CategoryNameAlreadyExistsException extends RuntimeException {
    public CategoryNameAlreadyExistsException(String name) {
        super("Category name already exists: " + name);
    }
}
