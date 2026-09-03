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
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private static final SecureRandom secureRandom = new SecureRandom();
    private static final long REFRESH_TOKEN_EXPIRY_DAYS = 30;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                              JwtService jwtService,
                              JwtProperties jwtProperties) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
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

    public RefreshResult validateAndRotate(String rawToken) {
        String hashedToken = hashToken(rawToken);
        RefreshToken existingToken = refreshTokenRepository.findByTokenHash(hashedToken)
                .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token not recognized"));

        // Reuse detection: an already-revoked token being presented again is
        // treated as theft. Revoke every token for the user and bail out here,
        // before any replacement is issued.
        if (existingToken.isRevoked()) {
            revokeAllForUser(existingToken.getUserId());
            throw new InvalidRefreshTokenException("Refresh token reuse detected");
        }

        // Expired-but-not-revoked token: reject before issuing anything new.
        if (existingToken.isExpired()) {
            throw new InvalidRefreshTokenException("Refresh token expired");
        }

        // Valid token: revoke it, then issue its replacement pair.
        existingToken.revoke();
        refreshTokenRepository.save(existingToken);

        UUID userId = existingToken.getUserId();
        String newAccessToken = jwtService.generateAccessToken(userId);
        String newRefreshToken = issueRefreshToken(userId);

        AuthTokens tokens = new AuthTokens(newAccessToken, newRefreshToken,
                jwtProperties.getAccessTokenExpiryMinutes());
        return new RefreshResult(userId, tokens);
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