package com.ihya.api.dailypractice;

import com.ihya.api.catalogue.Category;
import com.ihya.api.catalogue.Sunnah;
import com.ihya.api.catalogue.SunnahService;
import com.ihya.api.identity.User;
import com.ihya.api.identity.UserRepository;
import com.ihya.api.profile.ProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for {@link AssignmentService}: every collaborator is a
 * Mockito mock, the service is constructed by hand, no Spring context and no
 * database. Style matches {@code UserServiceTest}.
 */
@ExtendWith(MockitoExtension.class)
class AssignmentServiceTest {

    private static final LocalDate TODAY = LocalDate.now(ZoneId.of("UTC"));

    @Mock
    private DailyAssignmentRepository dailyAssignmentRepository;
    @Mock
    private PracticeRepository practiceRepository;
    @Mock
    private SunnahService sunnahService;
    @Mock
    private ProfileService profileService;
    @Mock
    private UserRepository userRepository;

    private AssignmentService assignmentService;

    @BeforeEach
    void setUp() {
        assignmentService = new AssignmentService(
                dailyAssignmentRepository, practiceRepository, sunnahService, profileService, userRepository);
    }

    // ----------------------------------------------------------------------
    // getTodayAssignment()
    // ----------------------------------------------------------------------

    @Test
    void getTodayAssignment_existingAssignmentToday_returnsItWithoutPickingAgain() {
        UUID userId = UUID.randomUUID();
        UUID sunnahId = UUID.randomUUID();
        DailyAssignment existing = new DailyAssignment(userId, TODAY, sunnahId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(userWithTimezone("UTC")));
        when(dailyAssignmentRepository.findByUserIdAndAssignmentDate(userId, TODAY)).thenReturn(Optional.of(existing));
        when(practiceRepository.findByUserIdAndPracticeDate(userId, TODAY)).thenReturn(Optional.empty());

        AssignmentResult result = assignmentService.getTodayAssignment(userId);

        assertThat(result.assignment()).isSameAs(existing);
        assertThat(result.replacementAvailable()).isTrue();
        verifyNoInteractions(sunnahService, profileService);
        verify(dailyAssignmentRepository, never()).insertIgnoringConflict(any(), any(), any());
    }

    @Test
    void getTodayAssignment_noAssignmentYetAndNoInterests_picksFirstEligibleInCuratedOrder() {
        UUID userId = UUID.randomUUID();
        Sunnah sunnahA = sunnahWithId(UUID.randomUUID(), "faith-worship");
        Sunnah sunnahB = sunnahWithId(UUID.randomUUID(), "social-manners");
        DailyAssignment created = new DailyAssignment(userId, TODAY, sunnahA.getId());
        when(userRepository.findById(userId)).thenReturn(Optional.of(userWithTimezone("UTC")));
        when(dailyAssignmentRepository.findByUserIdAndAssignmentDate(userId, TODAY))
                .thenReturn(Optional.empty(), Optional.of(created));
        when(practiceRepository.findByUserIdOrderByPracticeDateDesc(any(), any())).thenReturn(List.of());
        when(sunnahService.getAll()).thenReturn(List.of(sunnahA, sunnahB));
        when(profileService.getInterestSlugs(userId)).thenReturn(List.of());
        when(practiceRepository.findByUserIdAndPracticeDate(userId, TODAY)).thenReturn(Optional.empty());

        AssignmentResult result = assignmentService.getTodayAssignment(userId);

        assertThat(result.assignment().getSunnahId()).isEqualTo(sunnahA.getId());
        assertThat(result.replacementAvailable()).isTrue();
        verify(dailyAssignmentRepository).insertIgnoringConflict(userId, TODAY, sunnahA.getId());
    }

    @Test
    void getTodayAssignment_recentlyPracticedSunnahExcludedFromPick() {
        UUID userId = UUID.randomUUID();
        Sunnah sunnahA = sunnahWithId(UUID.randomUUID(), "faith-worship");
        Sunnah sunnahB = sunnahWithId(UUID.randomUUID(), "social-manners");
        Practice recentPractice = new Practice(userId, sunnahA.getId(), TODAY.minusDays(1), null);
        DailyAssignment created = new DailyAssignment(userId, TODAY, sunnahB.getId());
        when(userRepository.findById(userId)).thenReturn(Optional.of(userWithTimezone("UTC")));
        when(dailyAssignmentRepository.findByUserIdAndAssignmentDate(userId, TODAY))
                .thenReturn(Optional.empty(), Optional.of(created));
        when(practiceRepository.findByUserIdOrderByPracticeDateDesc(any(), any())).thenReturn(List.of(recentPractice));
        when(sunnahService.getAll()).thenReturn(List.of(sunnahA, sunnahB));
        when(profileService.getInterestSlugs(userId)).thenReturn(List.of());
        when(practiceRepository.findByUserIdAndPracticeDate(userId, TODAY)).thenReturn(Optional.empty());

        AssignmentResult result = assignmentService.getTodayAssignment(userId);

        assertThat(result.assignment().getSunnahId()).isEqualTo(sunnahB.getId());
    }

    @Test
    void getTodayAssignment_interestsNonEmptyWithOneEligibleCandidate_stillPicksIt() {
        // Only one Sunnah survives the exclusion window -- forces a deterministic
        // outcome even through the weighted-random branch, since weighting is
        // moot with a single candidate.
        UUID userId = UUID.randomUUID();
        Sunnah onlyEligible = sunnahWithId(UUID.randomUUID(), "faith-worship");
        DailyAssignment created = new DailyAssignment(userId, TODAY, onlyEligible.getId());
        when(userRepository.findById(userId)).thenReturn(Optional.of(userWithTimezone("UTC")));
        when(dailyAssignmentRepository.findByUserIdAndAssignmentDate(userId, TODAY))
                .thenReturn(Optional.empty(), Optional.of(created));
        when(practiceRepository.findByUserIdOrderByPracticeDateDesc(any(), any())).thenReturn(List.of());
        when(sunnahService.getAll()).thenReturn(List.of(onlyEligible));
        when(profileService.getInterestSlugs(userId)).thenReturn(List.of("faith-worship"));
        when(practiceRepository.findByUserIdAndPracticeDate(userId, TODAY)).thenReturn(Optional.empty());

        AssignmentResult result = assignmentService.getTodayAssignment(userId);

        assertThat(result.assignment().getSunnahId()).isEqualTo(onlyEligible.getId());
    }

    @Test
    void getTodayAssignment_concurrentInsertLosesRace_stillReturnsWinnersAssignment() {
        // insertIgnoringConflict reports 0 rows affected (the default for an
        // unstubbed int-returning mock) -- AssignmentService doesn't branch on
        // that return value at all, it always re-reads afterward, so the loser
        // of the race gets exactly the same correct result as the winner would.
        UUID userId = UUID.randomUUID();
        Sunnah sunnah = sunnahWithId(UUID.randomUUID(), "faith-worship");
        DailyAssignment winner = new DailyAssignment(userId, TODAY, UUID.randomUUID());
        when(userRepository.findById(userId)).thenReturn(Optional.of(userWithTimezone("UTC")));
        when(dailyAssignmentRepository.findByUserIdAndAssignmentDate(userId, TODAY))
                .thenReturn(Optional.empty(), Optional.of(winner));
        when(practiceRepository.findByUserIdOrderByPracticeDateDesc(any(), any())).thenReturn(List.of());
        when(sunnahService.getAll()).thenReturn(List.of(sunnah));
        when(profileService.getInterestSlugs(userId)).thenReturn(List.of());
        when(practiceRepository.findByUserIdAndPracticeDate(userId, TODAY)).thenReturn(Optional.empty());

        AssignmentResult result = assignmentService.getTodayAssignment(userId);

        assertThat(result.assignment()).isSameAs(winner);
    }

    // ----------------------------------------------------------------------
    // requestReplacement()
    // ----------------------------------------------------------------------

    @Test
    void requestReplacement_available_marksUsedStoresReasonAndPicksNewSunnah() {
        UUID userId = UUID.randomUUID();
        Sunnah current = sunnahWithId(UUID.randomUUID(), "faith-worship");
        Sunnah replacement = sunnahWithId(UUID.randomUUID(), "social-manners");
        DailyAssignment assignment = new DailyAssignment(userId, TODAY, current.getId());
        when(userRepository.findById(userId)).thenReturn(Optional.of(userWithTimezone("UTC")));
        when(dailyAssignmentRepository.findByUserIdAndAssignmentDate(userId, TODAY))
                .thenReturn(Optional.of(assignment));
        when(practiceRepository.findByUserIdAndPracticeDate(userId, TODAY)).thenReturn(Optional.empty());
        when(practiceRepository.findByUserIdOrderByPracticeDateDesc(any(), any())).thenReturn(List.of());
        when(sunnahService.getAll()).thenReturn(List.of(current, replacement));
        when(profileService.getInterestSlugs(userId)).thenReturn(List.of());

        AssignmentResult result = assignmentService.requestReplacement(userId, "not feeling it today");

        assertThat(assignment.getSunnahId()).isEqualTo(replacement.getId());
        assertThat(assignment.isReplacementUsed()).isTrue();
        assertThat(assignment.getReplacementReason()).isEqualTo("not feeling it today");
        assertThat(result.replacementAvailable()).isFalse();
        verify(dailyAssignmentRepository).save(assignment);
    }

    @Test
    void requestReplacement_alreadyUsed_throwsReplacementNotAvailable() {
        UUID userId = UUID.randomUUID();
        DailyAssignment assignment = new DailyAssignment(userId, TODAY, UUID.randomUUID());
        assignment.setReplacementUsed(true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(userWithTimezone("UTC")));
        when(dailyAssignmentRepository.findByUserIdAndAssignmentDate(userId, TODAY))
                .thenReturn(Optional.of(assignment));

        Throwable thrown = catchThrowable(() -> assignmentService.requestReplacement(userId, "reason"));

        assertThat(thrown).isInstanceOf(ReplacementNotAvailableException.class);
    }

    @Test
    void requestReplacement_alreadyPracticedToday_throwsReplacementNotAvailable() {
        UUID userId = UUID.randomUUID();
        DailyAssignment assignment = new DailyAssignment(userId, TODAY, UUID.randomUUID());
        Practice practicedToday = new Practice(userId, assignment.getSunnahId(), TODAY, null);
        when(userRepository.findById(userId)).thenReturn(Optional.of(userWithTimezone("UTC")));
        when(dailyAssignmentRepository.findByUserIdAndAssignmentDate(userId, TODAY))
                .thenReturn(Optional.of(assignment));
        when(practiceRepository.findByUserIdAndPracticeDate(userId, TODAY)).thenReturn(Optional.of(practicedToday));

        Throwable thrown = catchThrowable(() -> assignmentService.requestReplacement(userId, "reason"));

        assertThat(thrown).isInstanceOf(ReplacementNotAvailableException.class);
    }

    // ----------------------------------------------------------------------
    // deleteAllForUser()
    // ----------------------------------------------------------------------

    @Test
    void deleteAllForUser_delegatesToRepository() {
        UUID userId = UUID.randomUUID();

        assignmentService.deleteAllForUser(userId);

        verify(dailyAssignmentRepository).deleteAllByUserId(userId);
    }

    // ----------------------------------------------------------------------
    // helpers
    // ----------------------------------------------------------------------

    private static User userWithTimezone(String timezone) {
        User user = new User("assignment-test@example.com", "hash");
        user.setTimezone(timezone);
        return user;
    }

    private static Sunnah sunnahWithId(UUID id, String categorySlug) {
        Sunnah sunnah = new Sunnah(new Category(categorySlug, categorySlug, null, "active", 0),
                id.toString(), "Title", "Description", "Reflection", "Source", null, null, List.of());
        try {
            Field idField = Sunnah.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(sunnah, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not set Sunnah.id for test fixture", e);
        }
        return sunnah;
    }
}
