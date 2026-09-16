package com.ihya.api.dailypractice;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

// Composite key class for the (user_id, assignment_date) primary key.
// JPA requires this to be Serializable and to implement equals()/hashCode()
// over the same fields as the @Id fields on the entity.
public class DailyAssignmentId implements Serializable {

    private UUID userId;
    private LocalDate assignmentDate;

    public DailyAssignmentId() {
        // required by JPA
    }

    public DailyAssignmentId(UUID userId, LocalDate assignmentDate) {
        this.userId = userId;
        this.assignmentDate = assignmentDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DailyAssignmentId that)) {
            return false;
        }
        return Objects.equals(userId, that.userId) && Objects.equals(assignmentDate, that.assignmentDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, assignmentDate);
    }
}
