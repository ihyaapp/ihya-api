package com.ihya.api.dailypractice;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PracticeRepository extends JpaRepository<Practice, UUID> {

    Optional<Practice> findByUserIdAndPracticeDate(UUID userId, LocalDate practiceDate);

    /**
     * One practice per local day (docs/api-contract.md §2), enforced with
     * {@code ON CONFLICT DO NOTHING} rather than letting a unique-constraint
     * violation throw — see {@link PracticeService#recordPractice} for why a
     * thrown-and-caught exception doesn't work for this endpoint's conflict
     * case. {@code id}/{@code created_at} are left to their column defaults
     * ({@code gen_random_uuid()} / {@code now()}, V12). Returns the number of
     * rows actually inserted: {@code 1} on a fresh practice, {@code 0} when
     * today was already recorded.
     */
    @Modifying
    @Query(value = "INSERT INTO practices (user_id, sunnah_id, practice_date, feeling) "
            + "VALUES (:userId, :sunnahId, :practiceDate, :feeling) "
            + "ON CONFLICT (user_id, practice_date) DO NOTHING", nativeQuery = true)
    int insertIgnoringConflict(@Param("userId") UUID userId, @Param("sunnahId") UUID sunnahId,
            @Param("practiceDate") LocalDate practiceDate, @Param("feeling") String feeling);

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
