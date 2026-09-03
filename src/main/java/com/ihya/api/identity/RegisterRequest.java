package com.ihya.api.identity;

/**
 * Incoming payload for {@code POST /auth/register}.
 *
 * <p>A dedicated DTO — not the {@link User} entity — is what crosses the HTTP
 * boundary. The client supplies only an email and a raw password; the entity
 * carries fields a request must never set (id, password hash, created_at) and
 * that must never be serialized back to a client.
 *
 * <p>Contract (see {@code openapi/identity-api.yaml}): both fields are required,
 * {@code email} must be a well-formed address and {@code password} must be at
 * least 8 characters. Enforcing those rules arrives with the controller; this
 * type only fixes the shape.
 */
public record RegisterRequest(String email, String password) {
}
