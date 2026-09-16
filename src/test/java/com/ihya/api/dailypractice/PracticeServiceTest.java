package com.ihya.api.dailypractice;

import com.ihya.api.catalogue.Category;
import com.ihya.api.catalogue.Sunnah;
import com.ihya.api.catalogue.SunnahNotFoundException;
import com.ihya.api.catalogue.SunnahService;
import com.ihya.api.identity.User;
import com.ihya.api.identity.UserRepository;
import com.ihya.api.notification.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for {@link PracticeService}: every collaborator is a
 * Mockito mock, the service is constructed by hand, no Spring context and no
 * database. Style matches {@code UserServiceTest}.
 */
@ExtendWith(MockitoExtension.class)
class PracticeServiceTest {

    private static final LocalDate TODAY = LocalDate.now(ZoneId.of("UTC"));

    @Mock
    private PracticeRepository practiceRepository;
    @Mock
    private UserProgressService userProgressService;
    @Mock
    private SunnahService sunnahService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationService notificationService;

    private PracticeService practiceService;

    @BeforeEach
    void setUp() {
        practiceService = new PracticeService(
                practiceRepository, userProgressService, sunnahService, userRepository, notificationService);
    }

    // ----------------------------------------------------------------------
    // recordPractice()
    // ----------------------------------------------------------------------

    @Test
    void recordPractice_newPractice_savesAndReturnsCreatedResult() {
        UUID userId = UUID.randomUUID();
        UUID sunnahId = UUID.randomUUID();
        when(sunnahService.getById(sunnahId)).thenReturn(aSunnah());
        when(userRepository.findById(userId)).thenReturn(Optional.of(userWithTimezone("UTC")));
        when(practiceRepository.insertIgnoringConflict(userId, sunnahId, TODAY, "grateful")).thenReturn(1);
        Practice inserted = new Practice(userId, sunnahId, TODAY, "grateful");
        when(practiceRepository.findByUserIdAndPracticeDate(userId, TODAY)).thenReturn(Optional.of(inserted));
        UserProgress progress = new UserProgress(userId);
        progress.applyPractice(TODAY);
        when(userProgressService.recordPractice(userId, TODAY)).thenReturn(new ProgressUpdate(progress, 0, 0));

        PracticeRecordResult result = practiceService.recordPractice(userId, sunnahId, "grateful");

        assertThat(result.alreadyExisted()).isFalse();
        assertThat(result.practice().getFeeling()).isEqualTo("grateful");
        assertThat(result.practice().getPracticeDate()).isEqualTo(TODAY);
        assertThat(result.milestoneUnlocked()).isNull();
        assertThat(result.progress()).isSameAs(progress);
        verifyNoInteractions(notificationService);
    }

    @Test
    void recordPractice_writeCrossesMilestoneThreshold_returnsMilestoneKey() {
        UUID userId = UUID.randomUUID();
        UUID sunnahId = UUID.randomUUID();
        when(sunnahService.getById(sunnahId)).thenReturn(aSunnah());
        when(userRepository.findById(userId)).thenReturn(Optional.of(userWithTimezone("UTC")));
        when(practiceRepository.insertIgnoringConflict(userId, sunnahId, TODAY, null)).thenReturn(1);
        when(practiceRepository.findByUserIdAndPracticeDate(userId, TODAY))
                .thenReturn(Optional.of(new Practice(userId, sunnahId, TODAY, null)));
        UserProgress progress = new UserProgress(userId);
        progress.applyPractice(TODAY.minusDays(2));
        progress.applyPractice(TODAY.minusDays(1));
        progress.applyPractice(TODAY); // streak/longestStreak/totalPracticed now all 3
        when(userProgressService.recordPractice(userId, TODAY)).thenReturn(new ProgressUpdate(progress, 2, 2));

        PracticeRecordResult result = practiceService.recordPractice(userId, sunnahId, null);

        assertThat(result.milestoneUnlocked()).isEqualTo("streak_3");
        verify(notificationService).recordMilestoneEarned(userId, "streak_3");
    }

    @Test
    void recordPractice_alreadyPracticedToday_returnsExistingStateWithoutTouchingProgress() {
        UUID userId = UUID.randomUUID();
        UUID sunnahId = UUID.randomUUID();
        Practice existing = new Practice(userId, sunnahId, TODAY, "already recorded");
        UserProgress progress = new UserProgress(userId);
        when(sunnahService.getById(sunnahId)).thenReturn(aSunnah());
        when(userRepository.findById(userId)).thenReturn(Optional.of(userWithTimezone("UTC")));
        // 0 rows affected -- ON CONFLICT DO NOTHING skipped the insert, a row
        // already exists for today. Not an exception, just data.
        when(practiceRepository.insertIgnoringConflict(userId, sunnahId, TODAY, "different feeling")).thenReturn(0);
        when(practiceRepository.findByUserIdAndPracticeDate(userId, TODAY)).thenReturn(Optional.of(existing));
        when(userProgressService.getProgress(userId)).thenReturn(progress);

        PracticeRecordResult result = practiceService.recordPractice(userId, sunnahId, "different feeling");

        assertThat(result.alreadyExisted()).isTrue();
        assertThat(result.practice()).isSameAs(existing);
        assertThat(result.progress()).isSameAs(progress);
        assertThat(result.milestoneUnlocked()).isNull();
        verify(userProgressService, never()).recordPractice(any(), any());
    }

    @Test
    void recordPractice_unknownSunnah_propagatesSunnahNotFoundExceptionWithoutWriting() {
        UUID userId = UUID.randomUUID();
        UUID sunnahId = UUID.randomUUID();
        when(sunnahService.getById(sunnahId)).thenThrow(new SunnahNotFoundException(sunnahId));

        Throwable thrown = catchThrowable(() -> practiceService.recordPractice(userId, sunnahId, null));

        assertThat(thrown).isInstanceOf(SunnahNotFoundException.class);
        verifyNoInteractions(userRepository, userProgressService);
        verify(practiceRepository, never()).insertIgnoringConflict(any(), any(), any(), any());
    }

    // ----------------------------------------------------------------------
    // updateFeeling()
    // ----------------------------------------------------------------------

    @Test
    void updateFeeling_ownPractice_updatesAndSaves() {
        UUID userId = UUID.randomUUID();
        UUID practiceId = UUID.randomUUID();
        Practice practice = new Practice(userId, UUID.randomUUID(), TODAY, "old feeling");
        when(practiceRepository.findById(practiceId)).thenReturn(Optional.of(practice));
        when(practiceRepository.save(practice)).thenReturn(practice);

        Practice result = practiceService.updateFeeling(userId, practiceId, "new feeling");

        assertThat(result.getFeeling()).isEqualTo("new feeling");
        verify(practiceRepository).save(practice);
    }

    @Test
    void updateFeeling_unknownPractice_throwsPracticeNotFoundException() {
        UUID userId = UUID.randomUUID();
        UUID practiceId = UUID.randomUUID();
        when(practiceRepository.findById(practiceId)).thenReturn(Optional.empty());

        Throwable thrown = catchThrowable(() -> practiceService.updateFeeling(userId, practiceId, "feeling"));

        assertThat(thrown).isInstanceOf(PracticeNotFoundException.class);
    }

    @Test
    void updateFeeling_practiceOwnedByDifferentUser_throwsPracticeNotFoundException() {
        UUID practiceId = UUID.randomUUID();
        Practice practiceOfSomeoneElse = new Practice(UUID.randomUUID(), UUID.randomUUID(), TODAY, null);
        when(practiceRepository.findById(practiceId)).thenReturn(Optional.of(practiceOfSomeoneElse));

        Throwable thrown = catchThrowable(
                () -> practiceService.updateFeeling(UUID.randomUUID(), practiceId, "feeling"));

        assertThat(thrown).isInstanceOf(PracticeNotFoundException.class);
        verify(practiceRepository, never()).save(any());
    }

    // ----------------------------------------------------------------------
    // listPractices()
    // ----------------------------------------------------------------------

    @Test
    void listPractices_noCursorFewerThanLimit_returnsAllWithNullNextCursor() {
        UUID userId = UUID.randomUUID();
        List<Practice> practices = List.of(new Practice(userId, UUID.randomUUID(), TODAY, null));
        when(practiceRepository.findByUserIdOrderByPracticeDateDesc(eq(userId), any(Pageable.class)))
                .thenReturn(practices);

        PracticePage page = practiceService.listPractices(userId, null, 20);

        assertThat(page.items()).hasSize(1);
        assertThat(page.nextCursor()).isNull();
    }

    @Test
    void listPractices_moreRowsThanLimit_trimsToLimitAndSetsNextCursor() {
        UUID userId = UUID.randomUUID();
        Practice first = new Practice(userId, UUID.randomUUID(), TODAY, null);
        Practice second = new Practice(userId, UUID.randomUUID(), TODAY.minusDays(1), null);
        Practice third = new Practice(userId, UUID.randomUUID(), TODAY.minusDays(2), null); // the "extra" +1 row
        when(practiceRepository.findByUserIdOrderByPracticeDateDesc(eq(userId), any(Pageable.class)))
                .thenReturn(List.of(first, second, third));

        PracticePage page = practiceService.listPractices(userId, null, 2);

        assertThat(page.items()).containsExactly(first, second);
        assertThat(page.nextCursor()).isNotNull();
        assertThat(decodeCursor(page.nextCursor())).isEqualTo(TODAY.minusDays(1));
    }

    @Test
    void listPractices_withCursor_queriesStrictlyBeforeCursorDate() {
        UUID userId = UUID.randomUUID();
        LocalDate cursorDate = TODAY.minusDays(5);
        when(practiceRepository.findByUserIdAndPracticeDateLessThanOrderByPracticeDateDesc(
                eq(userId), eq(cursorDate), any(Pageable.class))).thenReturn(List.of());

        practiceService.listPractices(userId, encodeCursor(cursorDate), 20);

        verify(practiceRepository).findByUserIdAndPracticeDateLessThanOrderByPracticeDateDesc(
                eq(userId), eq(cursorDate), any(Pageable.class));
    }

    @Test
    void listPractices_malformedCursor_throwsIllegalArgumentException() {
        UUID userId = UUID.randomUUID();

        Throwable thrown = catchThrowable(() -> practiceService.listPractices(userId, "not-a-valid-cursor!!", 20));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }

    // ----------------------------------------------------------------------
    // getProgress() / deleteAllForUser()
    // ----------------------------------------------------------------------

    @Test
    void getProgress_delegatesToUserProgressService() {
        UUID userId = UUID.randomUUID();
        UserProgress progress = new UserProgress(userId);
        when(userProgressService.getProgress(userId)).thenReturn(progress);

        UserProgress result = practiceService.getProgress(userId);

        assertThat(result).isSameAs(progress);
    }

    @Test
    void deleteAllForUser_delegatesToRepository() {
        UUID userId = UUID.randomUUID();

        practiceService.deleteAllForUser(userId);

        verify(practiceRepository).deleteAllByUserId(userId);
    }

    // ----------------------------------------------------------------------
    // helpers
    // ----------------------------------------------------------------------

    private static User userWithTimezone(String timezone) {
        User user = new User("practice-test@example.com", "hash");
        user.setTimezone(timezone);
        return user;
    }

    private static Sunnah aSunnah() {
        return new Sunnah(new Category("faith-worship", "Faith & Worship", null, "active", 0),
                "a-sunnah", "Title", "Description", "Reflection", "Source", null, null, List.of());
    }

    private static String encodeCursor(LocalDate date) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(date.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static LocalDate decodeCursor(String cursor) {
        return LocalDate.parse(new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8));
    }
}
