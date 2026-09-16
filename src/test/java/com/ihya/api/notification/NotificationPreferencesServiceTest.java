package com.ihya.api.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for {@link NotificationPreferencesService}: the repository is
 * a Mockito mock, the service is constructed by hand, no Spring context and no
 * database. Style matches {@code CategoryServiceTest}.
 */
@ExtendWith(MockitoExtension.class)
class NotificationPreferencesServiceTest {

    @Mock
    private NotificationPreferencesRepository notificationPreferencesRepository;

    private NotificationPreferencesService notificationPreferencesService;

    @BeforeEach
    void setUp() {
        notificationPreferencesService = new NotificationPreferencesService(notificationPreferencesRepository);
    }

    @Test
    void createDefaults_savesRowWithContractDefaults() {
        UUID userId = UUID.randomUUID();
        ArgumentCaptor<NotificationPreferences> captor = ArgumentCaptor.forClass(NotificationPreferences.class);

        notificationPreferencesService.createDefaults(userId);

        verify(notificationPreferencesRepository).save(captor.capture());
        NotificationPreferences saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.isDailyReminder()).isTrue();
        assertThat(saved.isStreakReminder()).isTrue();
        assertThat(saved.isWeeklySummary()).isFalse();
        assertThat(saved.getReminderTime()).isEqualTo(LocalTime.of(9, 0));
    }

    @Test
    void getPreferences_existingRow_returnsIt() {
        UUID userId = UUID.randomUUID();
        NotificationPreferences preferences = new NotificationPreferences(userId);
        when(notificationPreferencesRepository.findById(userId)).thenReturn(Optional.of(preferences));

        NotificationPreferences result = notificationPreferencesService.getPreferences(userId);

        assertThat(result).isSameAs(preferences);
    }

    @Test
    void getPreferences_missingRow_throwsIllegalStateException() {
        UUID userId = UUID.randomUUID();
        when(notificationPreferencesRepository.findById(userId)).thenReturn(Optional.empty());

        Throwable thrown = catchThrowable(() -> notificationPreferencesService.getPreferences(userId));

        assertThat(thrown).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void updatePreferences_partialRequest_onlyChangesProvidedFields() {
        UUID userId = UUID.randomUUID();
        NotificationPreferences existing = new NotificationPreferences(userId);
        when(notificationPreferencesRepository.findById(userId)).thenReturn(Optional.of(existing));
        when(notificationPreferencesRepository.save(any(NotificationPreferences.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        NotificationPreferences result = notificationPreferencesService.updatePreferences(
                userId, false, null, null, null);

        assertThat(result.isDailyReminder()).isFalse();
        assertThat(result.isStreakReminder()).isTrue();
        assertThat(result.isWeeklySummary()).isFalse();
        assertThat(result.getReminderTime()).isEqualTo(LocalTime.of(9, 0));
    }

    @Test
    void updatePreferences_allFieldsProvided_appliesEveryChange() {
        UUID userId = UUID.randomUUID();
        NotificationPreferences existing = new NotificationPreferences(userId);
        when(notificationPreferencesRepository.findById(userId)).thenReturn(Optional.of(existing));
        when(notificationPreferencesRepository.save(any(NotificationPreferences.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        NotificationPreferences result = notificationPreferencesService.updatePreferences(
                userId, false, false, true, LocalTime.of(18, 30));

        assertThat(result.isDailyReminder()).isFalse();
        assertThat(result.isStreakReminder()).isFalse();
        assertThat(result.isWeeklySummary()).isTrue();
        assertThat(result.getReminderTime()).isEqualTo(LocalTime.of(18, 30));
    }

    @Test
    void deleteForUser_delegatesToRepository() {
        UUID userId = UUID.randomUUID();

        notificationPreferencesService.deleteForUser(userId);

        verify(notificationPreferencesRepository).deleteById(userId);
    }
}
