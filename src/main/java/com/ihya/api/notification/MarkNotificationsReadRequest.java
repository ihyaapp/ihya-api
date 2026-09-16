package com.ihya.api.notification;

import java.util.List;
import java.util.UUID;

/**
 * Incoming payload for {@code POST /notifications/read}. {@code ids} omitted,
 * {@code null}, or an empty list all mean "mark every unread notification as
 * read" (docs/api-contract.md §2 "Notifications") — the whole body is
 * optional for the same reason.
 */
public record MarkNotificationsReadRequest(List<UUID> ids) {
}
