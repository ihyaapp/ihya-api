package com.ihya.api.notification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Incoming payload for {@code POST /me/push-tokens}. Both fields are required —
 * there's no "current device" to fall back on.
 */
public record PushTokenRequest(
        @NotBlank String expoPushToken,
        @NotBlank @Pattern(regexp = "ios|android", message = "platform must be 'ios' or 'android'") String platform) {
}
