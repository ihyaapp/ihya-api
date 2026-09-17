package com.ihya.api.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for {@link NotificationService}: every collaborator is a
 * Mockito mock, the service is constructed by hand, no Spring context and no
 * database. Style matches {@code PracticeServiceTest}.
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(notificationRepository);
    }

    // ----------------------------------------------------------------------
    // recordMilestoneEarned()
    // ----------------------------------------------------------------------

    @Test
    void recordMilestoneEarned_savesNotificationWithMilestoneTypeAndTitleInBody() {
        UUID userId = UUID.randomUUID();
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);

        notificationService.recordMilestoneEarned(userId, "3 day streak");

        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getType()).isEqualTo("milestone_earned");
        assertThat(saved.getBody()).contains("3 day streak");
        assertThat(saved.isRead()).isFalse();
    }

    // ----------------------------------------------------------------------
    // listNotifications()
    // ----------------------------------------------------------------------

    @Test
    void listNotifications_noCursorFewerThanLimit_returnsAllWithNullNextCursor() {
        UUID userId = UUID.randomUUID();
        List<Notification> notifications = List.of(aNotification(userId));
        when(notificationRepository.findByUserIdOrderByCreatedAtDescIdDesc(eq(userId), any(Pageable.class)))
                .thenReturn(notifications);

        NotificationPage page = notificationService.listNotifications(userId, null, 20);

        assertThat(page.items()).hasSize(1);
        assertThat(page.nextCursor()).isNull();
    }

    @Test
    void listNotifications_moreRowsThanLimit_trimsToLimitAndSetsNextCursor() {
        UUID userId = UUID.randomUUID();
        Notification first = notificationWithId(userId, UUID.randomUUID());
        Notification second = notificationWithId(userId, UUID.randomUUID());
        Notification third = notificationWithId(userId, UUID.randomUUID()); // the "extra" +1 row
        when(notificationRepository.findByUserIdOrderByCreatedAtDescIdDesc(eq(userId), any(Pageable.class)))
                .thenReturn(List.of(first, second, third));

        NotificationPage page = notificationService.listNotifications(userId, null, 2);

        assertThat(page.items()).containsExactly(first, second);
        assertThat(page.nextCursor()).isNotNull();
        CursorPosition decoded = decodeCursor(page.nextCursor());
        assertThat(decoded.createdAt()).isEqualTo(second.getCreatedAt());
        assertThat(decoded.id()).isEqualTo(second.getId());
    }

    @Test
    void listNotifications_withCursor_queriesPageBeforeDecodedPosition() {
        UUID userId = UUID.randomUUID();
        Instant cursorCreatedAt = Instant.parse("2026-01-01T00:00:00Z");
        UUID cursorId = UUID.randomUUID();
        when(notificationRepository.findPageBefore(eq(userId), eq(cursorCreatedAt), eq(cursorId), any(Pageable.class)))
                .thenReturn(List.of());

        notificationService.listNotifications(userId, encodeCursor(cursorCreatedAt, cursorId), 20);

        verify(notificationRepository).findPageBefore(eq(userId), eq(cursorCreatedAt), eq(cursorId), any(Pageable.class));
    }

    @Test
    void listNotifications_malformedCursor_throwsIllegalArgumentException() {
        UUID userId = UUID.randomUUID();

        Throwable thrown = catchThrowable(() -> notificationService.listNotifications(userId, "not-a-valid-cursor!!", 20));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }

    // ----------------------------------------------------------------------
    // markRead()
    // ----------------------------------------------------------------------

    @Test
    void markRead_nullIds_marksAllAsRead() {
        UUID userId = UUID.randomUUID();

        notificationService.markRead(userId, null);

        verify(notificationRepository).markAllAsRead(eq(userId), any(Instant.class));
    }

    @Test
    void markRead_emptyIds_marksAllAsRead() {
        UUID userId = UUID.randomUUID();

        notificationService.markRead(userId, List.of());

        verify(notificationRepository).markAllAsRead(eq(userId), any(Instant.class));
    }

    @Test
    void markRead_withIds_marksOnlyThoseIds() {
        UUID userId = UUID.randomUUID();
        List<UUID> ids = List.of(UUID.randomUUID(), UUID.randomUUID());

        notificationService.markRead(userId, ids);

        verify(notificationRepository).markAsRead(eq(userId), eq(ids), any(Instant.class));
    }

    // ----------------------------------------------------------------------
    // deleteAllForUser()
    // ----------------------------------------------------------------------

    @Test
    void deleteAllForUser_delegatesToRepository() {
        UUID userId = UUID.randomUUID();

        notificationService.deleteAllForUser(userId);

        verify(notificationRepository).deleteAllByUserId(userId);
    }

    // ----------------------------------------------------------------------
    // helpers
    // ----------------------------------------------------------------------

    private static Notification aNotification(UUID userId) {
        return new Notification(userId, "milestone_earned", "Milestone unlocked!", "You've earned streak_3.");
    }

    /**
     * {@code id} is normally assigned by Hibernate's {@code GenerationType.UUID}
     * generator during a real {@code save()} — a Mockito mock never runs that,
     * so a plain {@code new Notification(...)} has a {@code null} id. The
     * {@code (createdAt, id)} cursor needs a real id to encode, so set it via
     * reflection, same as {@code UserServiceTest#userWithId}.
     */
    private static Notification notificationWithId(UUID userId, UUID id) {
        Notification notification = aNotification(userId);
        try {
            Field idField = Notification.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(notification, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not set Notification.id for test fixture", e);
        }
        return notification;
    }

    private static String encodeCursor(Instant createdAt, UUID id) {
        String raw = createdAt + "|" + id;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private static CursorPosition decodeCursor(String cursor) {
        String raw = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
        int separator = raw.indexOf('|');
        return new CursorPosition(Instant.parse(raw.substring(0, separator)), UUID.fromString(raw.substring(separator + 1)));
    }

    private record CursorPosition(Instant createdAt, UUID id) {
    }
}
