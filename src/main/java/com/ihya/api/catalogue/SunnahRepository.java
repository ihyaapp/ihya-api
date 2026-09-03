package com.ihya.api.catalogue;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface SunnahRepository extends JpaRepository<Sunnah, UUID> {

    /**
     * Optional title-contains + optional category filter, either independently
     * null. A single {@code @Query} with {@code (:param IS NULL OR ...)} guards
     * fits the interface-only, no-extra-classes style of the existing
     * repositories better than pulling in JPA Specifications for the first time.
     * The {@code query} argument is expected pre-trimmed and non-blank-or-null
     * (the service normalizes blank to null).
     */
    @Query("""
            SELECT s FROM Sunnah s
            WHERE (:query IS NULL OR LOWER(s.title) LIKE LOWER(CONCAT('%', :query, '%')))
              AND (:categoryId IS NULL OR s.category.id = :categoryId)
            """)
    Page<Sunnah> search(@Param("query") String query,
                        @Param("categoryId") UUID categoryId,
                        Pageable pageable);
}
