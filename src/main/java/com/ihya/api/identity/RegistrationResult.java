package com.ihya.api.identity;

/**
 * Result of a successful {@link UserService#register} call: the newly created
 * {@link User} paired with the {@link AuthTokens} issued for it.
 *
 * <p>Registration auto-logs-in the user (per the OpenAPI contract, {@code POST
 * /auth/register} responds with a full {@code AuthResponse}), so it hands back
 * tokens rather than making the client call {@code /auth/login} straight after.
 * Mirrors {@link LoginResult}.
 */
public record RegistrationResult(User user, AuthTokens tokens) {
}
