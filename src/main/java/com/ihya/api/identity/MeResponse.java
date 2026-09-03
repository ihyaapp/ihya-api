package com.ihya.api.identity;

import java.time.Instant;
import java.util.UUID;

/**
 * Public view of the authenticated user, returned by {@code GET /me}.
 *
 * <p>Like {@link AuthResponse}, this record exists so the {@link User} entity
 * never crosses the HTTP boundary: {@code User} carries {@code passwordHash},
 * which must never be serialised to a client. Only fields that are safe to
 * expose are copied here — the id, the email and the account creation time.
 */
public record MeResponse(UUID id, String email, Instant createdAt) {

    static MeResponse from(User user) {
        return new MeResponse(user.getId(), user.getEmail(), user.getCreatedAt());
    }
}
