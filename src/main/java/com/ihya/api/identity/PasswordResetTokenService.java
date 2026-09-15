package com.ihya.api.identity;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class PasswordResetTokenService {

    private static final long RESET_TOKEN_EXPIRY_MINUTES = 30;

    private final PasswordResetTokenRepository repository;

    public PasswordResetTokenService(PasswordResetTokenRepository repository) {
        this.repository = repository;
    }

    public String issueResetToken(UUID userId) {
        String rawToken = OpaqueTokenGenerator.generateRawToken();
        String hashedToken = OpaqueTokenGenerator.hashToken(rawToken);
        Instant expiresAt = Instant.now().plus(RESET_TOKEN_EXPIRY_MINUTES, ChronoUnit.MINUTES);
        repository.save(new PasswordResetToken(userId, hashedToken, expiresAt));
        return rawToken;
    }

    /**
     * Validates and consumes a reset token in one step, returning the user id
     * it belongs to. Every failure mode (not found, already used, expired)
     * throws the same generic message deliberately — distinguishing them in
     * the response would tell an attacker more than they should learn from a
     * failed reset attempt.
     */
    public UUID validateAndConsume(String rawToken) {
        String hashedToken = OpaqueTokenGenerator.hashToken(rawToken);
        PasswordResetToken token = repository.findByTokenHash(hashedToken)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired reset token"));

        if (token.isUsed() || token.isExpired()) {
            throw new IllegalArgumentException("Invalid or expired reset token");
        }

        token.markUsed();
        repository.save(token);
        return token.getUserId();
    }

    public void deleteAllForUser(UUID userId) {
        repository.deleteAllByUserId(userId);
    }
}
