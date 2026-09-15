package com.ihya.api.identity;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Incoming payload for {@code PATCH /me}.
 *
 * <p>Every field is optional — {@code null} means "leave unchanged" — so a
 * client can patch a single field without resending the rest. {@link Email}
 * and {@link Size} only fire on a non-null value, so {@code @Valid} still
 * allows a partial request through.
 */
public record UpdateMeRequest(
        @Size(max = 255) String name,
        @Email String email,
        String timezone,
        List<String> interests,
        Boolean personalizePromptDismissed) {
}
