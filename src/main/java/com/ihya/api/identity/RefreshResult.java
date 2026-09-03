package com.ihya.api.identity;

import java.util.UUID;

/**
 * Result of a successful {@link RefreshTokenService#validateAndRotate} call: the
 * id of the user the rotated token belongs to, paired with the freshly issued
 * {@link AuthTokens}.
 *
 * <p>Mirrors {@link LoginResult} / {@link RegistrationResult}, but deliberately
 * carries only the {@code userId} rather than a full {@link User}: the validated
 * refresh token already holds the user id, so loading the {@link User} here
 * would be exactly the redundant lookup those two methods exist to avoid.
 */
public record RefreshResult(UUID userId, AuthTokens tokens) {
}
