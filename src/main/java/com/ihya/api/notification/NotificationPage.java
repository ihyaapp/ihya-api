package com.ihya.api.notification;

import java.util.List;

/**
 * One cursor-paginated page of a user's notification feed
 * (docs/api-contract.md §0 "Pagination"). {@code nextCursor} is {@code null}
 * at the end of the feed.
 */
public record NotificationPage(List<Notification> items, String nextCursor) {
}
