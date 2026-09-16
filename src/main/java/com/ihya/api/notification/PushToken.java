package com.ihya.api.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "push_tokens")
public class PushToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "expo_push_token", nullable = false)
    private String expoPushToken;

    // "ios" | "android" -- validated at the request-DTO boundary, not here.
    @Column(name = "platform", nullable = false)
    private String platform;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PushToken() {
        // required by Hibernate
    }

    public PushToken(UUID userId, String expoPushToken, String platform) {
        this.userId = userId;
        this.expoPushToken = expoPushToken;
        this.platform = platform;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getExpoPushToken() {
        return expoPushToken;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
