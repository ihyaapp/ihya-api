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

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    // Required by Hibernate — used internally when loading rows from the DB.
    // You will not call this yourself.
    protected Category() {
    }

    // Used by your own code when creating a new category.
    public Category(String name, String description) {
        this.name = name;
        this.description = description;
        this.createdAt = Instant.now();
    }

    /**
     * Applies an edit from the admin catalogue. Not a raw setter: it is the one
     * intentional mutation this entity allows, and it enforces the same
     * non-blank rule on {@code name} that creation does. {@code name} is
     * trimmed; {@code description} stays optional.
     */
    public void update(String name, String description) {
        this.name = requireName(name);
        this.description = description;
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

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
