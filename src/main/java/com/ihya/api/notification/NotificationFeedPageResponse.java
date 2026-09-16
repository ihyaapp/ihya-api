package com.ihya.api.notification;

import java.util.List;

/** Client-facing shape for {@code GET /notifications} (docs/api-contract.md §0 "Pagination"). */
public record NotificationFeedPageResponse(List<NotificationResponse> items, String nextCursor) {
}
