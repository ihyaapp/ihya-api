package com.ihya.api.dailypractice;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "daily_assignments")
public class DailyAssignment {

    @EmbeddedId
    private DailyAssignmentId id;

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
        this.id = new DailyAssignmentId(userId, assignmentDate);
        this.sunnahId = sunnahId;
        this.replacementUsed = false;
        this.createdAt = Instant.now();
    }

    public DailyAssignmentId getId() {
        return id;
    }

    public UUID getUserId() {
        return id.getUserId();
    }

    public LocalDate getAssignmentDate() {
        return id.getAssignmentDate();
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
