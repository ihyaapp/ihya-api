package com.ihya.api.catalogue;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SunnahRepository extends JpaRepository<Sunnah, UUID> {

    Optional<Sunnah> findBySlug(String slug);

    long countByCategoryId(UUID categoryId);

    /**
     * Curated default order for the catalogue listing and (later) cold-start
     * assignment: by the parent category's {@code sort_order}, then by
     * creation order within it (docs/api-contract.md §2 "Selection logic").
     *
     * <p>{@code JOIN FETCH} loads each Sunnah's LAZY {@code category} in the
     * same query, instead of one extra SELECT per row when the controller
     * reads {@code categorySlug} for the response — the catalogue is small,
     * but there is no reason to pay an N+1 for a listing endpoint.
     */
    @Query("SELECT s FROM Sunnah s JOIN FETCH s.category ORDER BY s.category.sortOrder ASC, s.createdAt ASC")
    List<Sunnah> findAllOrderedByCategoryThenCreatedAt();
}
