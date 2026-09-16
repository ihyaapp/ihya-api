package com.ihya.api.dailypractice;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface DailyAssignmentRepository extends JpaRepository<DailyAssignment, DailyAssignmentId> {

    Optional<DailyAssignment> findByUserIdAndAssignmentDate(UUID userId, LocalDate assignmentDate);

    void deleteAllByUserId(UUID userId);
}
