package com.ihya.api.identity;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

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
 * least 8 characters. The Bean Validation annotations below enforce that
 * whenever a controller reads this with {@code @Valid}.
 */
public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, message = "password must be at least 8 characters") String password) {
}
