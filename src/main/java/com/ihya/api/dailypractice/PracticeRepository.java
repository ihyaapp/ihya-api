package com.ihya.api.dailypractice;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PracticeRepository extends JpaRepository<Practice, UUID> {

    Optional<Practice> findByUserIdAndPracticeDate(UUID userId, LocalDate practiceDate);

    // Recency window for assignment selection (docs/api-contract.md §2
    // "Selection logic" — exclude recently practiced Sunnahs). practice_date
    // is unique per user (UNIQUE(user_id, practice_date)), so ordering by it
    // descending is a stable "most recent first" walk with no tiebreaker
    // needed.
    List<Practice> findByUserIdOrderByPracticeDateDesc(UUID userId, Pageable pageable);

    // Cursor pagination for GET /practices: the same uniqueness property
    // makes practice_date itself a valid, strictly-ordered opaque cursor.
    List<Practice> findByUserIdAndPracticeDateLessThanOrderByPracticeDateDesc(
            UUID userId, LocalDate cursor, Pageable pageable);

    void deleteAllByUserId(UUID userId);
}
