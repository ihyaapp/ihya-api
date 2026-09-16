package com.ihya.api.dailypractice;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "practices")
public class Practice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "sunnah_id", nullable = false)
    private UUID sunnahId;

    @Column(name = "practice_date", nullable = false)
    private LocalDate practiceDate;

    @Column(name = "feeling")
    private String feeling;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Practice() {
        // required by Hibernate
    }

    public Practice(UUID userId, UUID sunnahId, LocalDate practiceDate, String feeling) {
        this.userId = userId;
        this.sunnahId = sunnahId;
        this.practiceDate = practiceDate;
        this.feeling = feeling;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getSunnahId() {
        return sunnahId;
    }

    public LocalDate getPracticeDate() {
        return practiceDate;
    }

    public String getFeeling() {
        return feeling;
    }

    public void setFeeling(String feeling) {
        this.feeling = feeling;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
