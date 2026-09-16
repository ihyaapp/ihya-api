package com.ihya.api.dailypractice;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface DailyAssignmentRepository extends JpaRepository<DailyAssignment, DailyAssignmentId> {

    Optional<DailyAssignment> findByUserIdAndAssignmentDate(UUID userId, LocalDate assignmentDate);

    /**
     * One assignment per local day, enforced with {@code ON CONFLICT DO NOTHING}
     * rather than a thrown-and-caught unique-constraint violation — see
     * {@link AssignmentService#createAssignment} for why. {@code replacement_used}
     * / {@code created_at} are left to their column defaults (V12).
     */
    @Modifying
    @Query(value = "INSERT INTO daily_assignments (user_id, assignment_date, sunnah_id) "
            + "VALUES (:userId, :assignmentDate, :sunnahId) "
            + "ON CONFLICT (user_id, assignment_date) DO NOTHING", nativeQuery = true)
    int insertIgnoringConflict(@Param("userId") UUID userId, @Param("assignmentDate") LocalDate assignmentDate,
            @Param("sunnahId") UUID sunnahId);

    void deleteAllByUserId(UUID userId);
}
