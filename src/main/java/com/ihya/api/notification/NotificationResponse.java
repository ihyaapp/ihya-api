package com.ihya.api.notification;

import java.time.Instant;
import java.util.UUID;

/** Client-facing shape for a {@link Notification} (docs/api-contract.md §1 "Notification"). */
public record NotificationResponse(UUID id, String type, String title, String body, Instant createdAt, boolean read) {

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(), notification.getType(), notification.getTitle(), notification.getBody(),
                notification.getCreatedAt(), notification.isRead());
    }
}
