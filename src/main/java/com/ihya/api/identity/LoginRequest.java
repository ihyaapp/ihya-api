package com.ihya.api.identity;

/**
 * Incoming payload for {@code POST /auth/login}.
 *
 * <p>Contract (see {@code openapi/identity-api.yaml}): both fields are required.
 * Unlike {@link RegisterRequest} there is no password-length rule here — login
 * checks the supplied password against the stored hash, not against the
 * registration password policy.
 */
public record LoginRequest(String email, String password) {
}
