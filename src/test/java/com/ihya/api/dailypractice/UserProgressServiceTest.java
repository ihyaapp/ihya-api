package com.ihya.api.dailypractice;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for {@link UserProgressService}: the repository is a
 * Mockito mock, the service is constructed by hand, no Spring context and no
 * database. Style matches {@code NotificationPreferencesServiceTest}.
 */
@ExtendWith(MockitoExtension.class)
class UserProgressServiceTest {

    @Mock
    private UserProgressRepository userProgressRepository;

    private UserProgressService userProgressService;

    @BeforeEach
    void setUp() {
        userProgressService = new UserProgressService(userProgressRepository);
    }

    @Test
    void createDefaults_savesFreshRowForUser() {
        UUID userId = UUID.randomUUID();
        ArgumentCaptor<UserProgress> captor = ArgumentCaptor.forClass(UserProgress.class);

        userProgressService.createDefaults(userId);

        verify(userProgressRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(userId);
        assertThat(captor.getValue().getStreak()).isZero();
        assertThat(captor.getValue().getTotalPracticed()).isZero();
    }

    @Test
    void getProgress_existingRow_returnsIt() {
        UUID userId = UUID.randomUUID();
        UserProgress progress = new UserProgress(userId);
        when(userProgressRepository.findById(userId)).thenReturn(Optional.of(progress));

        UserProgress result = userProgressService.getProgress(userId);

        assertThat(result).isSameAs(progress);
    }

    @Test
    void getProgress_missingRow_throwsIllegalStateException() {
        UUID userId = UUID.randomUUID();
        when(userProgressRepository.findById(userId)).thenReturn(Optional.empty());

        Throwable thrown = catchThrowable(() -> userProgressService.getProgress(userId));

        assertThat(thrown).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void recordPractice_appliesPracticeAndReturnsBeforeAndAfterState() {
        UUID userId = UUID.randomUUID();
        UserProgress progress = new UserProgress(userId);
        progress.applyPractice(LocalDate.of(2026, 9, 1));
        progress.applyPractice(LocalDate.of(2026, 9, 2)); // streak = 2, longest = 2, total = 2
        when(userProgressRepository.findById(userId)).thenReturn(Optional.of(progress));

        ProgressUpdate update = userProgressService.recordPractice(userId, LocalDate.of(2026, 9, 3));

        assertThat(update.previousLongestStreak()).isEqualTo(2);
        assertThat(update.previousTotalPracticed()).isEqualTo(2);
        assertThat(update.progress().getStreak()).isEqualTo(3);
        assertThat(update.progress().getLongestStreak()).isEqualTo(3);
        assertThat(update.progress().getTotalPracticed()).isEqualTo(3);
        verify(userProgressRepository).save(progress);
    }

    @Test
    void deleteForUser_delegatesToRepository() {
        UUID userId = UUID.randomUUID();

        userProgressService.deleteForUser(userId);

        verify(userProgressRepository).deleteById(userId);
    }
}
