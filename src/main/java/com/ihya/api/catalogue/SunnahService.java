package com.ihya.api.catalogue;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Business operations over {@link Sunnah}.
 *
 * <p>Same conventions as {@link CategoryService}: blank {@code title} /
 * {@code description} / {@code action} &rarr; {@link IllegalArgumentException};
 * unknown Sunnah id &rarr; {@link SunnahNotFoundException}; a {@code categoryId}
 * that names no category &rarr; {@link CategoryNotFoundException} (reused, not a
 * new type — it is the same "that category does not exist" condition).
 *
 * <p>{@code update} re-resolves and re-validates the target category exactly as
 * {@code create} does, then applies the edit through {@link Sunnah#update}.
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
    public Sunnah create(UUID categoryId, String title, String description, String action, String reference) {
        String cleanTitle = requireText(title, "title");
        String cleanDescription = requireText(description, "description");
        String cleanAction = requireText(action, "action");
        Category category = requireCategory(categoryId);

        Sunnah sunnah = new Sunnah(category, cleanTitle, cleanDescription, cleanAction, trimToNull(reference));
        return sunnahRepository.save(sunnah);
    }

    public Sunnah getById(UUID id) {
        return sunnahRepository.findById(id)
                .orElseThrow(() -> new SunnahNotFoundException(id));
    }

    /**
     * Offset (page/size) pagination over the catalogue. {@code query} is an
     * optional case-insensitive title-contains match; {@code categoryId} is an
     * optional exact filter; each may be null independently. A blank
     * {@code query} is treated as absent.
     */
    public Page<Sunnah> search(String query, UUID categoryId, Pageable pageable) {
        return sunnahRepository.search(trimToNull(query), categoryId, pageable);
    }

    @Transactional
    public Sunnah update(UUID id, String title, String description, String action, String reference, UUID categoryId) {
        Sunnah sunnah = getById(id);
        Category category = requireCategory(categoryId);
        sunnah.update(title, description, action, reference, category);
        return sunnahRepository.save(sunnah);
    }

    @Transactional
    public void delete(UUID id) {
        Sunnah sunnah = getById(id);
        sunnahRepository.delete(sunnah);
    }

    private Category requireCategory(UUID categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));
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
}
