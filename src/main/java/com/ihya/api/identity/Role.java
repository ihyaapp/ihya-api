package com.ihya.api.identity;

/**
 * Authorization role carried by every {@link User}.
 *
 * <p>Reads across the app are open to any authenticated user; writes to
 * admin-managed content (the Week 2 Sunnah catalogue) are restricted to
 * {@link #ADMIN}. Self-registration always produces {@link #USER} — see
 * {@link User#User(String, String)}. There is no in-app way to change a role;
 * the first admin is provisioned by a direct database update.
 *
 * <p>Persisted by name via {@code @Enumerated(EnumType.STRING)}, never by
 * ordinal, so the declaration order here is not load-bearing.
 */
public enum Role {
    USER,
    ADMIN
}
