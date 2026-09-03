package com.ihya.api.profile;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "profiles")

public class Profile {
    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "name")
    private String name;

    protected Profile() {
        // required by Hibernate
    }

    public Profile(UUID userId) {
        this.userId = userId;
        this.name = null;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
