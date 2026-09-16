package com.ihya.api.notification;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByUserIdOrderByCreatedAtDescIdDesc(UUID userId, Pageable pageable);

    /**
     * Cursor pagination for {@code GET /notifications}. Unlike
     * {@code PracticeRepository}'s {@code practice_date} cursor, {@code
     * created_at} alone isn't guaranteed unique per user (no unique
     * constraint backs it), so a plain "less than cursor" comparison could
     * skip or repeat rows whenever two notifications land in the same
     * instant. Ordering and comparing on the {@code (created_at, id)} pair
     * instead keeps the page strictly ordered even on a timestamp tie.
     */
    @Query("SELECT n FROM Notification n WHERE n.userId = :userId "
            + "AND (n.createdAt < :createdAt OR (n.createdAt = :createdAt AND n.id < :id)) "
            + "ORDER BY n.createdAt DESC, n.id DESC")
    List<Notification> findPageBefore(@Param("userId") UUID userId, @Param("createdAt") Instant createdAt,
            @Param("id") UUID id, Pageable pageable);

    @Modifying
    @Query("UPDATE Notification n SET n.readAt = :now WHERE n.userId = :userId AND n.readAt IS NULL")
    int markAllAsRead(@Param("userId") UUID userId, @Param("now") Instant now);

    @Modifying
    @Query("UPDATE Notification n SET n.readAt = :now WHERE n.userId = :userId AND n.id IN :ids AND n.readAt IS NULL")
    int markAsRead(@Param("userId") UUID userId, @Param("ids") List<UUID> ids, @Param("now") Instant now);

    void deleteAllByUserId(UUID userId);
}
