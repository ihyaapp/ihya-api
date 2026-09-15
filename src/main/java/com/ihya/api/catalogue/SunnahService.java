package com.ihya.api.catalogue;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Business operations over {@link Sunnah}.
 *
 * <p>Same conventions as {@link CategoryService}: blank {@code slug} /
 * {@code title} / {@code description} / {@code reflection} / {@code source}
 * &rarr; {@link IllegalArgumentException}; unknown Sunnah id/slug &rarr;
 * {@link SunnahNotFoundException}; a {@code categorySlug} that names no
 * category &rarr; {@link CategoryNotFoundException} (reused, not a new type —
 * it is the same "that category does not exist" condition); duplicate
 * {@code slug} &rarr; {@link SunnahSlugAlreadyExistsException} via the
 * persist-and-remap pattern, not a check-then-insert.
 *
 * <p>{@code slug} is immutable once created; {@code update} re-resolves and
 * re-validates a reassigned category exactly as {@code create} does, then
 * applies the edit through {@link Sunnah#update}. {@link #getById} is kept
 * alongside {@link #getBySlug} because the daily-practice module (Phase 5)
 * will resolve a Sunnah by the opaque {@code sunnahId} carried on
 * {@code Practice} / {@code Assignment}, not by slug.
 */
@Service
public class SunnahService {

    private final SunnahRepository sunnahRepository;
    private final CategoryRepository categoryRepository;

    public SunnahService(SunnahRepository sunnahRepository, CategoryRepository categoryRepository) {
        this.sunnahRepository = sunnahRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public Sunnah create(SunnahCreateRequest request) {
        String slug = requireText(request.slug(), "slug");
        String title = requireText(request.title(), "title");
        String description = requireText(request.description(), "description");
        String reflection = requireText(request.reflection(), "reflection");
        String source = requireText(request.source(), "source");
        Category category = requireCategoryBySlug(request.categorySlug());
        List<String> tags = request.tags() == null ? List.of() : request.tags();

        Sunnah sunnah = new Sunnah(category, slug, title, description, reflection, source,
                trimToNull(request.arabicText()), trimToNull(request.prompt()), tags);
        return persist(sunnah);
    }

    public Sunnah getById(UUID id) {
        return sunnahRepository.findById(id)
                .orElseThrow(() -> new SunnahNotFoundException(id));
    }

    public Sunnah getBySlug(String slug) {
        return sunnahRepository.findBySlug(slug)
                .orElseThrow(() -> new SunnahNotFoundException(slug));
    }

    /** Full catalogue listing in the curated default order — see {@link SunnahRepository}. */
    public List<Sunnah> getAll() {
        return sunnahRepository.findAllOrderedByCategoryThenCreatedAt();
    }

    public long countByCategory(UUID categoryId) {
        return sunnahRepository.countByCategoryId(categoryId);
    }

    @Transactional
    public Sunnah update(String slug, SunnahUpdateRequest request) {
        Sunnah sunnah = getBySlug(slug);

        String title = request.title() != null ? requireText(request.title(), "title") : sunnah.getTitle();
        String description = request.description() != null
                ? requireText(request.description(), "description") : sunnah.getDescription();
        String reflection = request.reflection() != null
                ? requireText(request.reflection(), "reflection") : sunnah.getReflection();
        String source = request.source() != null ? requireText(request.source(), "source") : sunnah.getSource();
        String arabicText = request.arabicText() != null ? trimToNull(request.arabicText()) : sunnah.getArabicText();
        String prompt = request.prompt() != null ? trimToNull(request.prompt()) : sunnah.getPrompt();
        List<String> tags = request.tags() != null ? request.tags() : sunnah.getTags();
        Category category = request.categorySlug() != null
                ? requireCategoryBySlug(request.categorySlug()) : sunnah.getCategory();

        sunnah.update(title, description, reflection, source, arabicText, prompt, tags, category);
        return persist(sunnah);
    }

    @Transactional
    public void delete(String slug) {
        Sunnah sunnah = getBySlug(slug);
        sunnahRepository.delete(sunnah);
    }

    private Category requireCategoryBySlug(String categorySlug) {
        return categoryRepository.findBySlug(categorySlug)
                .orElseThrow(() -> new CategoryNotFoundException(categorySlug));
    }

    /**
     * saveAndFlush (not save): force the unique-constraint check to happen
     * here, inside the try. A {@code sunnahs_slug_key} violation is remapped
     * to {@link SunnahSlugAlreadyExistsException}; anything else propagates.
     * Shared by create and update, mirroring {@link CategoryService#persist}.
     */
    private Sunnah persist(Sunnah sunnah) {
        try {
            return sunnahRepository.saveAndFlush(sunnah);
        } catch (DataIntegrityViolationException ex) {
            if (isUniqueViolation(ex, "sunnahs_slug_key")) {
                throw new SunnahSlugAlreadyExistsException(sunnah.getSlug());
            }
            throw ex;
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static boolean isUniqueViolation(DataIntegrityViolationException ex, String constraintName) {
        Throwable cause = ex.getMostSpecificCause();
        String message = cause == null ? null : cause.getMessage();
        return message != null && message.toLowerCase(Locale.ROOT).contains(constraintName);
    }
}
