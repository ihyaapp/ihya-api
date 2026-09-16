package com.ihya.api.notification;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * The in-app notification feed (docs/api-contract.md §2 "Notifications").
 *
 * <p>The only producer in v1 is {@link #recordMilestoneEarned}, called inline
 * by {@code PracticeService.recordPractice} in the same transaction as the
 * practice that unlocked the milestone — no scheduler yet, see
 * docs/api-contract.md §5.
 */
@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    /**
     * {@code milestoneKey} is one of {@code MilestoneEvaluator}'s placeholder
     * keys (docs/api-contract.md §5) — the body text below inherits that same
     * "not confirmed against ihya-mobile/src/constants/milestones.ts" caveat.
     */
    public void recordMilestoneEarned(UUID userId, String milestoneKey) {
        notificationRepository.save(new Notification(userId, "milestone_earned", "Milestone unlocked!",
                "You've earned the " + milestoneKey + " milestone. Keep it up!"));
    }

    public NotificationPage listNotifications(UUID userId, String cursor, int limit) {
        CursorPosition position = cursor == null ? null : decodeCursor(cursor);
        List<Notification> rows = position == null
                ? notificationRepository.findByUserIdOrderByCreatedAtDescIdDesc(userId, PageRequest.of(0, limit + 1))
                : notificationRepository.findPageBefore(
                        userId, position.createdAt(), position.id(), PageRequest.of(0, limit + 1));

        boolean hasMore = rows.size() > limit;
        List<Notification> page = hasMore ? rows.subList(0, limit) : rows;
        String nextCursor = hasMore ? encodeCursor(page.get(page.size() - 1)) : null;
        return new NotificationPage(page, nextCursor);
    }

    /**
     * {@code ids} omitted or empty marks every unread notification as read;
     * otherwise only the given ids. {@code @Transactional} because the
     * {@code @Modifying} bulk updates below ({@code markAllAsRead}/{@code
     * markAsRead}) require an active transaction to run at all — same
     * requirement {@code PracticeService.recordPractice} satisfies for its
     * own {@code @Modifying} insert.
     */
    @Transactional
    public void markRead(UUID userId, List<UUID> ids) {
        Instant now = Instant.now();
        if (ids == null || ids.isEmpty()) {
            notificationRepository.markAllAsRead(userId, now);
        } else {
            notificationRepository.markAsRead(userId, ids, now);
        }
    }

    public void deleteAllForUser(UUID userId) {
        notificationRepository.deleteAllByUserId(userId);
    }

    private static String encodeCursor(Notification notification) {
        String raw = notification.getCreatedAt() + "|" + notification.getId();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private static CursorPosition decodeCursor(String cursor) {
        try {
            String raw = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            int separator = raw.indexOf('|');
            Instant createdAt = Instant.parse(raw.substring(0, separator));
            UUID id = UUID.fromString(raw.substring(separator + 1));
            return new CursorPosition(createdAt, id);
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Invalid cursor", ex);
        }
    }

    private record CursorPosition(Instant createdAt, UUID id) {
    }
}
