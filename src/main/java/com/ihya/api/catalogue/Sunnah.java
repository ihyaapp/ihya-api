package com.ihya.api.catalogue;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sunnahs")
public class Sunnah {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // LAZY on purpose: a Sunnah is often listed/loaded without needing the full
    // Category. optional = false + a NOT NULL join column mirror the DB FK.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "action", nullable = false)
    private String action;

    @Column(name = "reference")
    private String reference;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    // Required by Hibernate — used internally when loading rows from the DB.
    // You will not call this yourself.
    protected Sunnah() {
    }

    // Used by your own code when creating a new Sunnah.
    public Sunnah(Category category, String title, String description, String action, String reference) {
        this.category = category;
        this.title = title;
        this.description = description;
        this.action = action;
        this.reference = reference;
        this.createdAt = Instant.now();
    }

    /**
     * Applies an edit from the admin catalogue. Not a raw setter: the one
     * intentional mutation, enforcing the same non-blank rules on
     * {@code title} / {@code description} / {@code action} that creation does.
     * Those three are trimmed; {@code reference} stays optional (blank becomes
     * null); {@code category} is the reassigned parent, already resolved and
     * validated by the caller.
     */
    public void update(String title, String description, String action, String reference, Category category) {
        this.title = requireText(title, "title");
        this.description = requireText(description, "description");
        this.action = requireText(action, "action");
        this.reference = trimToNull(reference);
        this.category = category;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    // Getters — no plain setters; the only mutation is update() above.
    public UUID getId() {
        return id;
    }

    public Category getCategory() {
        return category;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getAction() {
        return action;
    }

    public String getReference() {
        return reference;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
