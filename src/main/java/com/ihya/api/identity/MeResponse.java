package com.ihya.api.identity;

import com.ihya.api.profile.Profile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Public view of the authenticated user, returned by {@code GET /me}.
 *
 * <p>Like {@link AuthResponse}, this record exists so the {@link User} entity
 * never crosses the HTTP boundary: {@code User} carries {@code passwordHash},
 * which must never be serialised to a client. Only fields that are safe to
 * expose are copied here — the id, the email and the account creation time.
 *
 * <p>It also composes across the profile module the same way: {@link Profile}
 * fields (name, personalizePromptDismissed) and the caller's interest slugs
 * are folded in here rather than exposed as a separate profile-shaped payload.
 */
public record MeResponse(UUID id, String email, String name, String timezone, List<String> interests,
                          boolean personalizePromptDismissed, Instant createdAt) {

    static MeResponse from(User user, Profile profile, List<String> interests) {
        return new MeResponse(
                user.getId(),
                user.getEmail(),
                profile.getName(),
                user.getTimezone(),
                interests,
                profile.isPersonalizePromptDismissed(),
                user.getCreatedAt());
    }
}
