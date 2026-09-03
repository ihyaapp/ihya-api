package com.ihya.api.identity;

/**
 * Result of a successful {@link UserService#login} call: the authenticated
 * {@link User} paired with the freshly issued {@link AuthTokens}.
 *
 * <p>{@code login()} already loads the full user to verify the password, so it
 * hands that user back rather than forcing callers (the auth controller) into a
 * second database lookup just to read the id. {@link AuthTokens} stays a
 * standalone type for flows that never need a user — e.g. token refresh.
 */
public record LoginResult(User user, AuthTokens tokens) {
}
