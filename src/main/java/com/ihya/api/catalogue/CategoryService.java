package com.ihya.api.catalogue;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Business operations over the Sunnah-catalogue {@link Category}.
 *
 * <p>Validation and duplicate handling follow the identity module:
 * <ul>
 *   <li>blank {@code slug} / {@code name}, or an unrecognized {@code status}
 *       &rarr; {@link IllegalArgumentException};</li>
 *   <li>duplicate {@code name} &rarr; {@link CategoryNameAlreadyExistsException};
 *       duplicate {@code slug} &rarr; {@link CategorySlugAlreadyExistsException}
 *       &mdash; both by catching the matching unique-constraint violation and
 *       remapping it, exactly as {@code UserService} does for email — no
 *       check-then-insert race in application code;</li>
 *   <li>unknown slug &rarr; {@link CategoryNotFoundException}.</li>
 * </ul>
 *
 * <p>{@code slug} is immutable once created (seed-data-spec.md: "never re-slug"),
 * so it is never accepted by {@link #update}. {@code create} and {@code update}
 * share the same persist-and-remap step, so renaming a category onto a name
 * that already exists fails exactly as a colliding create does.
 */
@Service
public class CategoryService {

    private static final Set<String> VALID_STATUSES = Set.of("active", "coming-soon");
    private static final String DEFAULT_STATUS = "active";

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public Category create(CategoryCreateRequest request) {
        String slug = requireText(request.slug(), "slug");
        String name = requireText(request.name(), "name");
        String status = requireStatus(request.status() == null ? DEFAULT_STATUS : request.status());
        return persist(new Category(slug, name, request.description(), status, 0));
    }

    public Category getBySlug(String slug) {
        return categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new CategoryNotFoundException(slug));
    }

    public List<Category> getAll() {
        return categoryRepository.findAll();
    }

    @Transactional
    public Category update(String slug, CategoryUpdateRequest request) {
        Category category = getBySlug(slug);
        String name = request.name() != null ? request.name() : category.getName();
        String description = request.description() != null ? request.description() : category.getDescription();
        String status = request.status() != null ? requireStatus(request.status()) : category.getStatus();
        category.update(name, description, status, category.getSortOrder());
        return persist(category);
    }

    @Transactional
    public void delete(String slug) {
        Category category = getBySlug(slug);
        categoryRepository.delete(category);
    }

    /**
     * saveAndFlush (not save): make the unique-constraint check happen here,
     * inside the try, rather than at transaction commit after the caller
     * returns. A {@code categories_name_key} or {@code categories_slug_key}
     * violation is remapped to the matching typed exception; anything else
     * propagates. Mirrors {@code UserService.register}, and is shared by
     * create and update so a rename collision behaves like a create collision.
     */
    private Category persist(Category category) {
        try {
            return categoryRepository.saveAndFlush(category);
        } catch (DataIntegrityViolationException ex) {
            if (isUniqueViolation(ex, "categories_name_key")) {
                throw new CategoryNameAlreadyExistsException(category.getName());
            }
            if (isUniqueViolation(ex, "categories_slug_key")) {
                throw new CategorySlugAlreadyExistsException(category.getSlug());
            }
            throw ex;
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Category " + field + " must not be blank");
        }
        return value.trim();
    }

    private static String requireStatus(String status) {
        String normalized = status == null ? null : status.trim().toLowerCase(Locale.ROOT);
        if (!VALID_STATUSES.contains(normalized)) {
            throw new IllegalArgumentException(
                    "Category status must be one of " + VALID_STATUSES + ", was: " + status);
        }
        return normalized;
    }

    /**
     * True only when the violation is the named unique constraint. Anything
     * else (a future constraint, a real integrity bug) must propagate rather
     * than be masked as a duplicate. Same technique as
     * {@code UserService.isEmailUniqueViolation}.
     */
    private static boolean isUniqueViolation(DataIntegrityViolationException ex, String constraintName) {
        Throwable cause = ex.getMostSpecificCause();
        String message = cause == null ? null : cause.getMessage();
        return message != null && message.toLowerCase(Locale.ROOT).contains(constraintName);
    }
}
