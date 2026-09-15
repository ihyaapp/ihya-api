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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "sunnahs")
public class Sunnah {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Stable, human-facing key -- see Category.slug. Immutable once created.
    @Column(name = "slug", nullable = false, unique = true)
    private String slug;

    // LAZY on purpose: a Sunnah is often listed/loaded without needing the full
    // Category. optional = false + a NOT NULL join column mirror the DB FK.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", nullable = false)
    private String description;

    // "Try this today" instruction. Renamed from `action` (V9) to match the
    // client-visible field name.
    @Column(name = "reflection", nullable = false)
    private String reflection;

    // Display citation, e.g. "Sahih al-Bukhari 24". Renamed from `reference`
    // (V9); NOT NULL as of that migration -- every Sunnah must cite a source.
    @Column(name = "source", nullable = false)
    private String source;

    // Full tashkeel; only ever populated once verified against a second
    // source (ihya-mobile/docs/arabic-review.md). Null means "English-only".
    @Column(name = "arabic_text")
    private String arabicText;

    // Short reflective/curiosity question, distinct from `reflection`. Sparse.
    @Column(name = "prompt")
    private String prompt;

    // Reserved for seasonal targeting (e.g. "ramadan"); not read by v1
    // assignment logic. Maps to a Postgres text[], never null (defaults to {}).
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "tags", nullable = false, columnDefinition = "text[]")
    private List<String> tags = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    // Required by Hibernate — used internally when loading rows from the DB.
    // You will not call this yourself.
    protected Sunnah() {
    }

    // Used by your own code when creating a new Sunnah.
    public Sunnah(Category category, String slug, String title, String description, String reflection,
                  String source, String arabicText, String prompt, List<String> tags) {
        this.category = category;
        this.slug = slug;
        this.title = title;
        this.description = description;
        this.reflection = reflection;
        this.source = source;
        this.arabicText = arabicText;
        this.prompt = prompt;
        this.tags = tags == null ? new ArrayList<>() : new ArrayList<>(tags);
        this.createdAt = Instant.now();
    }

    /**
     * Applies an edit from the admin catalogue. Not a raw setter: the one
     * intentional mutation (besides {@code slug}, which is immutable),
     * enforcing the same non-blank rules on {@code title} / {@code description}
     * / {@code reflection} / {@code source} that creation does. Those four are
     * trimmed; {@code arabicText} / {@code prompt} stay optional (blank becomes
     * null); {@code category} is the reassigned parent, already resolved and
     * validated by the caller.
     */
    public void update(String title, String description, String reflection, String source, String arabicText,
                       String prompt, List<String> tags, Category category) {
        this.title = requireText(title, "title");
        this.description = requireText(description, "description");
        this.reflection = requireText(reflection, "reflection");
        this.source = requireText(source, "source");
        this.arabicText = trimToNull(arabicText);
        this.prompt = trimToNull(prompt);
        this.tags = tags == null ? new ArrayList<>() : new ArrayList<>(tags);
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

    public String getSlug() {
        return slug;
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

    public String getReflection() {
        return reflection;
    }

    public String getSource() {
        return source;
    }

    public String getArabicText() {
        return arabicText;
    }

    public String getPrompt() {
        return prompt;
    }

    public List<String> getTags() {
        return tags;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
