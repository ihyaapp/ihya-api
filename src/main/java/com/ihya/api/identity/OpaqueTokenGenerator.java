package com.ihya.api.identity;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Generates opaque bearer tokens (refresh tokens, password-reset tokens):
 * a random 64-byte value returned to the caller, paired with its SHA-256
 * hash for storage — the raw value is never persisted, only ever handed
 * to the client once at issuance.
 */
final class OpaqueTokenGenerator {

    private static final SecureRandom secureRandom = new SecureRandom();

    private OpaqueTokenGenerator() {
    }

    static String generateRawToken() {
        byte[] randomBytes = new byte[64];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    static String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes());
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
