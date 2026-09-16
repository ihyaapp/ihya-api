package com.ihya.api.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for {@link PushTokenService}: the repository is a Mockito
 * mock, the service is constructed by hand, no Spring context and no database.
 * Style matches {@code CategoryServiceTest}.
 */
@ExtendWith(MockitoExtension.class)
class PushTokenServiceTest {

    @Mock
    private PushTokenRepository pushTokenRepository;

    private PushTokenService pushTokenService;

    @BeforeEach
    void setUp() {
        pushTokenService = new PushTokenService(pushTokenRepository);
    }

    @Test
    void registerToken_newToken_persistsIt() {
        UUID userId = UUID.randomUUID();
        when(pushTokenRepository.findByUserIdAndExpoPushToken(userId, "ExponentPushToken[abc]"))
                .thenReturn(Optional.empty());
        ArgumentCaptor<PushToken> captor = ArgumentCaptor.forClass(PushToken.class);

        pushTokenService.registerToken(userId, "ExponentPushToken[abc]", "ios");

        verify(pushTokenRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(userId);
        assertThat(captor.getValue().getExpoPushToken()).isEqualTo("ExponentPushToken[abc]");
        assertThat(captor.getValue().getPlatform()).isEqualTo("ios");
    }

    @Test
    void registerToken_alreadyRegisteredForUser_isANoOp() {
        UUID userId = UUID.randomUUID();
        when(pushTokenRepository.findByUserIdAndExpoPushToken(userId, "ExponentPushToken[abc]"))
                .thenReturn(Optional.of(new PushToken(userId, "ExponentPushToken[abc]", "android")));

        pushTokenService.registerToken(userId, "ExponentPushToken[abc]", "android");

        verify(pushTokenRepository, never()).saveAndFlush(any());
    }

    @Test
    void registerToken_concurrentRegistrationHitsUniqueConstraint_isSwallowed() {
        UUID userId = UUID.randomUUID();
        when(pushTokenRepository.findByUserIdAndExpoPushToken(userId, "ExponentPushToken[abc]"))
                .thenReturn(Optional.empty());
        when(pushTokenRepository.saveAndFlush(any(PushToken.class))).thenThrow(new DataIntegrityViolationException(
                "could not execute statement [ERROR: duplicate key value violates unique "
                        + "constraint \"push_tokens_user_id_expo_push_token_key\"]"));

        assertThatCode(() -> pushTokenService.registerToken(userId, "ExponentPushToken[abc]", "ios"))
                .doesNotThrowAnyException();
    }

    @Test
    void registerToken_unrelatedConstraintViolation_propagates() {
        UUID userId = UUID.randomUUID();
        DataIntegrityViolationException dbError = new DataIntegrityViolationException(
                "could not execute statement [ERROR: insert or update on table \"push_tokens\" violates "
                        + "foreign key constraint \"some_other_fk\"]");
        when(pushTokenRepository.findByUserIdAndExpoPushToken(userId, "ExponentPushToken[abc]"))
                .thenReturn(Optional.empty());
        when(pushTokenRepository.saveAndFlush(any(PushToken.class))).thenThrow(dbError);

        Throwable thrown = catchThrowable(() -> pushTokenService.registerToken(userId, "ExponentPushToken[abc]", "ios"));

        assertThat(thrown).isSameAs(dbError);
    }

    @Test
    void deleteAllForUser_delegatesToRepository() {
        UUID userId = UUID.randomUUID();

        pushTokenService.deleteAllForUser(userId);

        verify(pushTokenRepository).deleteAllByUserId(userId);
    }
}
