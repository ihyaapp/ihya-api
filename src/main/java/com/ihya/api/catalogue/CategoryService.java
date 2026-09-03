package com.ihya.api.catalogue;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Business operations over the Sunnah-catalogue {@link Category}.
 *
 * <p>Validation and duplicate handling follow the identity module:
 * <ul>
 *   <li>blank name &rarr; {@link IllegalArgumentException} (identity validates at
 *       the DTO boundary with {@code @NotBlank}; there is no boundary here yet,
 *       so the service guards itself — see the task report note on this choice);</li>
 *   <li>duplicate name &rarr; {@link CategoryNameAlreadyExistsException}, by
 *       catching the {@code categories_name_key} unique-constraint violation and
 *       remapping it, exactly as {@code UserService} does for email — no
 *       check-then-insert race in application code;</li>
 *   <li>unknown id &rarr; {@link CategoryNotFoundException}.</li>
 * </ul>
 *
 * <p>{@code create} and {@code update} share the same persist-and-remap step, so
 * renaming a category onto a name that already exists fails exactly as a
 * colliding create does.
 */
@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public Category create(String name, String description) {
        String cleanName = requireName(name);
        return persist(new Category(cleanName, description));
    }

    public Category getById(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(id));
    }

    public List<Category> getAll() {
        return categoryRepository.findAll();
    }

    @Transactional
    public Category update(UUID id, String name, String description) {
        Category category = getById(id);
        category.update(name, description);
        return persist(category);
    }

    @Transactional
    public void delete(UUID id) {
        Category category = getById(id);
        categoryRepository.delete(category);
    }

    /**
     * saveAndFlush (not save): make the unique-constraint check happen here,
     * inside the try, rather than at transaction commit after the caller
     * returns. A {@code categories_name_key} violation is remapped to
     * {@link CategoryNameAlreadyExistsException}; anything else propagates.
     * Mirrors {@code UserService.register}, and is shared by create and update
     * so a rename collision behaves like a create collision.
     */
    private Category persist(Category category) {
        try {
            return categoryRepository.saveAndFlush(category);
        } catch (DataIntegrityViolationException ex) {
            if (isCategoryNameUniqueViolation(ex)) {
                throw new CategoryNameAlreadyExistsException(category.getName());
            }
            throw ex;
        }
    }

    private static String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Category name must not be blank");
        }
        return name.trim();
    }

    /**
     * True only when the violation is the {@code categories.name} unique
     * constraint. Anything else (a future constraint, a real integrity bug) must
     * propagate rather than be masked as a duplicate name. Same technique as
     * {@code UserService.isEmailUniqueViolation}.
     */
    private static boolean isCategoryNameUniqueViolation(DataIntegrityViolationException ex) {
        Throwable cause = ex.getMostSpecificCause();
        String message = cause == null ? null : cause.getMessage();
        return message != null && message.toLowerCase(Locale.ROOT).contains("categories_name_key");
    }
}
