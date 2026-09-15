package com.ihya.api.profile;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "user_interests")
@IdClass(UserInterest.UserInterestId.class)
public class UserInterest {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Id
    @Column(name = "category_slug")
    private String categorySlug;

    protected UserInterest() {
        // required by Hibernate
    }

    public UserInterest(UUID userId, String categorySlug) {
        this.userId = userId;
        this.categorySlug = categorySlug;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getCategorySlug() {
        return categorySlug;
    }

    // Composite key class for the (user_id, category_slug) primary key.
    // JPA requires this to be Serializable and to implement equals()/hashCode()
    // over the same fields as the @Id fields on the entity.
    public static class UserInterestId implements Serializable {

        private UUID userId;
        private String categorySlug;

        public UserInterestId() {
            // required by JPA
        }

        public UserInterestId(UUID userId, String categorySlug) {
            this.userId = userId;
            this.categorySlug = categorySlug;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof UserInterestId that)) {
                return false;
            }
            return Objects.equals(userId, that.userId) && Objects.equals(categorySlug, that.categorySlug);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, categorySlug);
        }
    }
}
