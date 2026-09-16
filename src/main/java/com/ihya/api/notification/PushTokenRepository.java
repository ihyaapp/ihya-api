package com.ihya.api.notification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PushTokenRepository extends JpaRepository<PushToken, UUID> {

    Optional<PushToken> findByUserIdAndExpoPushToken(UUID userId, String expoPushToken);
    void deleteAllByUserId(UUID userId);
}
