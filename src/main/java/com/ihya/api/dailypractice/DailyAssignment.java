package com.ihya.api.dailypractice;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "daily_assignments")
@IdClass(DailyAssignmentId.class)
public class DailyAssignment {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Id
    @Column(name = "assignment_date")
    private LocalDate assignmentDate;

    @Column(name = "sunnah_id", nullable = false)
    private UUID sunnahId;

    @Column(name = "replacement_used", nullable = false)
    private boolean replacementUsed;

    @Column(name = "replacement_reason")
    private String replacementReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected DailyAssignment() {
        // required by Hibernate
    }

    public DailyAssignment(UUID userId, LocalDate assignmentDate, UUID sunnahId) {
        this.userId = userId;
        this.assignmentDate = assignmentDate;
        this.sunnahId = sunnahId;
        this.replacementUsed = false;
        this.createdAt = Instant.now();
    }

    public UUID getUserId() {
        return userId;
    }

    public LocalDate getAssignmentDate() {
        return assignmentDate;
    }

    public UUID getSunnahId() {
        return sunnahId;
    }

    public void setSunnahId(UUID sunnahId) {
        this.sunnahId = sunnahId;
    }

    public boolean isReplacementUsed() {
        return replacementUsed;
    }

    public void setReplacementUsed(boolean replacementUsed) {
        this.replacementUsed = replacementUsed;
    }

    public String getReplacementReason() {
        return replacementReason;
    }

    public void setReplacementReason(String replacementReason) {
        this.replacementReason = replacementReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
