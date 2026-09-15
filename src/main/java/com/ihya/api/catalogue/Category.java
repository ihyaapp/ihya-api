package com.ihya.api.catalogue;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Stable, human-facing key. Client payloads reference a category by this,
    // never by id (docs/api-contract.md §0 "Category reference"). Immutable
    // once created -- see the seed-data-spec.md rule "never re-slug".
    @Column(name = "slug", nullable = false, unique = true)
    private String slug;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "description")
    private String description;

    // "active" | "coming-soon" -- mirrors the categories_status check
    // constraint (V9); validated at the CategoryService boundary, not here.
    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    // Required by Hibernate — used internally when loading rows from the DB.
    // You will not call this yourself.
    protected Category() {
    }

    // Used by your own code when creating a new category.
    public Category(String slug, String name, String description, String status, int sortOrder) {
        this.slug = slug;
        this.name = name;
        this.description = description;
        this.status = status;
        this.sortOrder = sortOrder;
        this.createdAt = Instant.now();
    }

    /**
     * Applies an edit from the admin catalogue. Not a raw setter: it is the one
     * intentional mutation this entity allows (besides {@code slug}, which is
     * immutable), and it enforces the same non-blank rule on {@code name} that
     * creation does. {@code name} is trimmed; {@code description} stays
     * optional; {@code status} is expected already-validated by the caller.
     */
    public void update(String name, String description, String status, int sortOrder) {
        this.name = requireName(name);
        this.description = description;
        this.status = status;
        this.sortOrder = sortOrder;
    }

    private static String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Category name must not be blank");
        }
        return name.trim();
    }

    // Getters — no plain setters; the only mutation is update() above.
    public UUID getId() {
        return id;
    }

    public String getSlug() {
        return slug;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getStatus() {
        return status;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
