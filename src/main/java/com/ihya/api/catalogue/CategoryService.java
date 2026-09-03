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
 * <p>{@code update(...)} is intentionally not implemented yet: {@link Category}
 * exposes no mutation path (no setters, no domain method) and adding one is the
 * open decision flagged in the report. Everything else is complete.
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

        Category category = new Category(cleanName, description);
        try {
            // saveAndFlush (not save): make the unique-constraint check happen
            // here, inside the try, rather than at transaction commit after this
            // method returns. Mirrors UserService.register.
            return categoryRepository.saveAndFlush(category);
        } catch (DataIntegrityViolationException ex) {
            if (isCategoryNameUniqueViolation(ex)) {
                throw new CategoryNameAlreadyExistsException(cleanName);
            }
            throw ex;
        }
    }

    public Category getById(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(id));
    }

    public List<Category> getAll() {
        return categoryRepository.findAll();
    }

    @Transactional
    public void delete(UUID id) {
        Category category = getById(id);
        categoryRepository.delete(category);
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
