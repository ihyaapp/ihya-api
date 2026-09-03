package com.ihya.api.identity;

/**
 * The id carried by an otherwise-valid access token matches no user row — i.e.
 * the account was deleted after the token was issued.
 *
 * <p>Mapped to <strong>401</strong> by {@link GlobalExceptionHandler}, not 404:
 * the token still verifies cryptographically but no longer names a real
 * principal, so the request is effectively unauthenticated and the client must
 * re-authenticate. (Re-login is the only recovery and cannot succeed for a
 * deleted account — which is the intended outcome.) This mirrors the existing
 * convention in {@link UserService#login}, where a missing user is also surfaced
 * as a 401 ({@link InvalidCredentialsException}) rather than a 404.
 */
public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException() {
        super("Authenticated user no longer exists");
    }
}
