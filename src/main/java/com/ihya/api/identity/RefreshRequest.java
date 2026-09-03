package com.ihya.api.identity;

import jakarta.validation.constraints.NotBlank;

/**
 * Incoming payload for {@code POST /auth/refresh}.
 *
 * <p>Contract (see {@code openapi/identity-api.yaml}): the OpenAPI schema marks
 * this request body optional because it also describes a browser flow that
 * carries the refresh token in an HttpOnly cookie. Ihya is Android-only for
 * Month 1 — there is no cookie flow — so every caller is a "mobile client",
 * and the same doc states that for mobile clients {@code refreshToken} is
 * REQUIRED and its absence is a validation error. Hence {@code @NotBlank}
 * rather than an optional field.
 */
public record RefreshRequest(
        @NotBlank String refreshToken) {
}
