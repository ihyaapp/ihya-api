package com.ihya.api.identity;

import com.ihya.api.profile.Profile;

import java.util.List;

/**
 * Result of a successful {@link UserService#updateMe} call: the updated
 * {@link User}, the updated {@link Profile}, and the caller's current
 * interest slugs. Mirrors {@link RegistrationResult}/{@link LoginResult}.
 */
public record UpdateMeResult(User user, Profile profile, List<String> interests) {
}
