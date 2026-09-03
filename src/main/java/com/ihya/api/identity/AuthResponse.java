package com.ihya.api.identity;

import java.util.UUID;

/**
 * Response body for {@code POST /auth/register}, {@code POST /auth/login} and
 * {@code POST /auth/refresh}.
 *
 * <p>This is the outward-facing counterpart to the service-layer
 * {@link AuthTokens} record, and it deliberately differs from it:
 * <ul>
 *   <li>{@code expiresIn} is the access-token lifetime in <strong>seconds</strong>
 *       (per the API contract), whereas
 *       {@link AuthTokens#accessTokenExpiryMinutes()} is in minutes — the
 *       controller converts.</li>
 *   <li>{@code userId} is surfaced for the client; {@link AuthTokens} does not
 *       carry it.</li>
 *   <li>{@code refreshToken} is populated for mobile clients only. Web clients
 *       receive it as an HttpOnly cookie and see {@code null} here.</li>
 * </ul>
 *
 * <p>Field order matches the {@code AuthResponse} schema in
 * {@code openapi/identity-api.yaml}.
 */
public record AuthResponse(String accessToken, long expiresIn, String refreshToken, UUID userId) {
}
