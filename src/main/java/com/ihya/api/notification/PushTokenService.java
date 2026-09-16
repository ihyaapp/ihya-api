package com.ihya.api.notification;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.UUID;

/**
 * Registers a device's Expo push token against a user.
 *
 * <p>Unlike the identity/catalogue uniqueness cases, a duplicate
 * {@code (user_id, expo_push_token)} isn't an error here — re-registering the
 * same device is expected (app relaunch, token refresh confirmation) and the
 * API contract has no {@code 409} for this endpoint. So the unique-constraint
 * violation is remapped to a silent no-op rather than a thrown exception.
 */
@Service
public class PushTokenService {

    private final PushTokenRepository pushTokenRepository;

    public PushTokenService(PushTokenRepository pushTokenRepository) {
        this.pushTokenRepository = pushTokenRepository;
    }

    public void registerToken(UUID userId, String expoPushToken, String platform) {
        if (pushTokenRepository.findByUserIdAndExpoPushToken(userId, expoPushToken).isPresent()) {
            return;
        }
        try {
            pushTokenRepository.saveAndFlush(new PushToken(userId, expoPushToken, platform));
        } catch (DataIntegrityViolationException ex) {
            if (!isPushTokenUniqueViolation(ex)) {
                throw ex;
            }
            // Lost the race to a concurrent registration of the same token — the
            // row already exists, which is exactly what this call wanted.
        }
    }

    public void deleteAllForUser(UUID userId) {
        pushTokenRepository.deleteAllByUserId(userId);
    }

    private static boolean isPushTokenUniqueViolation(DataIntegrityViolationException ex) {
        Throwable cause = ex.getMostSpecificCause();
        String message = cause == null ? null : cause.getMessage();
        return message != null
                && message.toLowerCase(Locale.ROOT).contains("push_tokens_user_id_expo_push_token_key");
    }
}
