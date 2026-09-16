package com.ihya.api.dailypractice;

import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class DailyAssignmentId implements Serializable {

    private UUID userId;
    private LocalDate assignmentDate;

    protected DailyAssignmentId() {
        // required by Hibernate
    }

    public DailyAssignmentId(UUID userId, LocalDate assignmentDate) {
        this.userId = userId;
        this.assignmentDate = assignmentDate;
    }

    public UUID getUserId() {
        return userId;
    }

    public LocalDate getAssignmentDate() {
        return assignmentDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DailyAssignmentId that)) return false;
        return Objects.equals(userId, that.userId) && Objects.equals(assignmentDate, that.assignmentDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, assignmentDate);
    }
}
