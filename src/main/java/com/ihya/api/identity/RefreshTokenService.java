package com.ihya.api.identity;

import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private static final SecureRandom secureRandom = new SecureRandom();
    private static final long REFRESH_TOKEN_EXPIRY_DAYS = 30;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public String issueRefreshToken(UUID userId) {
        String rawToken = generateRawToken();
        String hashedToken = hashToken(rawToken);

        Instant expiresAt = Instant.now().plus(REFRESH_TOKEN_EXPIRY_DAYS, ChronoUnit.DAYS);
        RefreshToken refreshToken = new RefreshToken(userId, hashedToken, expiresAt);
        refreshTokenRepository.save(refreshToken);

        return rawToken;
    }

    private String generateRawToken() {
        byte[] randomBytes = new byte[64];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes());
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    public RefreshToken validateAndRotate(String rawToken) {
        String hashedToken = hashToken(rawToken);
        RefreshToken existingToken = refreshTokenRepository.findByTokenHash(hashedToken)
                .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token not recognized"));

        if (existingToken.isRevoked()) {
            revokeAllForUser(existingToken.getUserId());
            throw new InvalidRefreshTokenException("Refresh token reuse detected");
        }

        if (existingToken.isExpired()) {
            throw new InvalidRefreshTokenException("Refresh token expired");
        }

        existingToken.revoke();
        refreshTokenRepository.save(existingToken);

        return existingToken;
    }

    public void revokeAllForUser(UUID userId) {
        List<RefreshToken> activeTokens = refreshTokenRepository.findAllByUserIdAndRevokedAtIsNull(userId);
        for (RefreshToken token : activeTokens) {
            token.revoke();
        }
        refreshTokenRepository.saveAll(activeTokens);
    }


    public void revokeToken(String rawToken) {
        String hashedToken = hashToken(rawToken);
        refreshTokenRepository.findByTokenHash(hashedToken)
                .ifPresent(token -> {
                    if (!token.isRevoked()) {
                        token.revoke();
                        refreshTokenRepository.save(token);
                    }
                });
    }
}