package com.ihya.api.dailypractice;

import com.ihya.api.catalogue.Sunnah;
import com.ihya.api.catalogue.SunnahService;
import com.ihya.api.identity.UserNotFoundException;
import com.ihya.api.identity.UserRepository;
import com.ihya.api.profile.ProfileService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Resolves and replaces the user's assigned Sunnah for their current local
 * day (docs/api-contract.md §2 "Selection logic").
 *
 * <p>Depends on {@link UserRepository} directly rather than
 * {@code UserService} for the caller's timezone — {@code UserService.deleteMe}
 * depends on this service (via {@link #deleteAllForUser}) to clean up
 * daily-practice data, so going through {@code UserService} here would create
 * a circular Spring bean dependency. {@link UserRepository} is a plain
 * Spring Data interface with no dependencies of its own, so reading the
 * timezone straight off it breaks the cycle without needing a new module.
 */
@Service
public class AssignmentService {

    // How many of the most recently practiced Sunnahs are excluded from
    // today's pick — enough variety that a ~30-Sunnah catalogue with a narrow
    // interest set doesn't repeat within a week.
    private static final int RECENT_EXCLUSION_WINDOW = 7;

    // Relative selection weight for a Sunnah in one of the user's interest
    // categories vs. one outside it, when interests is non-empty.
    private static final double INTEREST_WEIGHT = 3.0;
    private static final double DEFAULT_WEIGHT = 1.0;

    private final DailyAssignmentRepository dailyAssignmentRepository;
    private final PracticeRepository practiceRepository;
    private final SunnahService sunnahService;
    private final ProfileService profileService;
    private final UserRepository userRepository;

    public AssignmentService(DailyAssignmentRepository dailyAssignmentRepository,
                              PracticeRepository practiceRepository,
                              SunnahService sunnahService,
                              ProfileService profileService,
                              UserRepository userRepository) {
        this.dailyAssignmentRepository = dailyAssignmentRepository;
        this.practiceRepository = practiceRepository;
        this.sunnahService = sunnahService;
        this.profileService = profileService;
        this.userRepository = userRepository;
    }

    @Transactional
    public AssignmentResult getTodayAssignment(UUID userId) {
        LocalDate today = resolveLocalDate(userId);
        DailyAssignment assignment = dailyAssignmentRepository.findByUserIdAndAssignmentDate(userId, today)
                .orElseGet(() -> createAssignment(userId, today));
        return toResult(userId, assignment, today);
    }

    @Transactional
    public AssignmentResult requestReplacement(UUID userId, String reason) {
        LocalDate today = resolveLocalDate(userId);
        DailyAssignment assignment = dailyAssignmentRepository.findByUserIdAndAssignmentDate(userId, today)
                .orElseGet(() -> createAssignment(userId, today));

        if (assignment.isReplacementUsed()) {
            throw ReplacementNotAvailableException.alreadyUsed();
        }
        if (practiceRepository.findByUserIdAndPracticeDate(userId, today).isPresent()) {
            throw ReplacementNotAvailableException.alreadyPracticedToday();
        }

        Set<UUID> excluded = recentSunnahIds(userId);
        excluded.add(assignment.getSunnahId());
        UUID newSunnahId = pickSunnah(userId, excluded);

        assignment.setSunnahId(newSunnahId);
        assignment.setReplacementUsed(true);
        assignment.setReplacementReason(reason);
        dailyAssignmentRepository.save(assignment);

        return toResult(userId, assignment, today);
    }

    public void deleteAllForUser(UUID userId) {
        dailyAssignmentRepository.deleteAllByUserId(userId);
    }

    /**
     * {@code INSERT ... ON CONFLICT DO NOTHING} ({@link
     * DailyAssignmentRepository#insertIgnoringConflict}), not persist-and-catch:
     * two simultaneous first-visits-of-the-day for the same user can both miss
     * the {@code findByUserIdAndAssignmentDate} check above and both reach
     * here. A thrown-and-caught unique-constraint violation doesn't work for
     * this — Postgres refuses any further command on a transaction after one
     * statement in it has thrown, and Spring separately marks the whole
     * ambient transaction rollback-only the instant that exception is thrown,
     * so even a fresh transaction for the retry still leaves the outer one
     * doomed at commit (see {@code PracticeService.recordPractice}, which hit
     * exactly this). {@code ON CONFLICT DO NOTHING} avoids the problem
     * entirely: the loser just re-reads the row the winner created, in the
     * same healthy transaction.
     */
    private DailyAssignment createAssignment(UUID userId, LocalDate date) {
        UUID sunnahId = pickSunnah(userId, recentSunnahIds(userId));
        dailyAssignmentRepository.insertIgnoringConflict(userId, date, sunnahId);
        return dailyAssignmentRepository.findByUserIdAndAssignmentDate(userId, date)
                .orElseThrow(() -> new IllegalStateException(
                        "daily_assignments row missing immediately after insert for user " + userId));
    }

    private LocalDate resolveLocalDate(UUID userId) {
        String timezone = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new)
                .getTimezone();
        return LocalDate.now(ZoneId.of(timezone));
    }

    private Set<UUID> recentSunnahIds(UUID userId) {
        return new HashSet<>(practiceRepository
                .findByUserIdOrderByPracticeDateDesc(userId, PageRequest.of(0, RECENT_EXCLUSION_WINDOW))
                .stream().map(Practice::getSunnahId).toList());
    }

    private UUID pickSunnah(UUID userId, Set<UUID> excludedSunnahIds) {
        List<Sunnah> catalogue = sunnahService.getAll();
        List<Sunnah> eligible = catalogue.stream()
                .filter(sunnah -> !excludedSunnahIds.contains(sunnah.getId()))
                .toList();
        if (eligible.isEmpty()) {
            // Only reachable if the exclusion window ever grows past the
            // catalogue size — fall back to the full catalogue rather than fail.
            eligible = catalogue;
        }

        List<String> interests = profileService.getInterestSlugs(userId);
        if (interests.isEmpty()) {
            // Deterministic curated order (docs/api-contract.md §2): eligible
            // is already sorted by category.sortOrder then createdAt.
            return eligible.get(0).getId();
        }

        return weightedRandomPick(eligible, interests).getId();
    }

    private static Sunnah weightedRandomPick(List<Sunnah> candidates, List<String> interests) {
        double[] weights = new double[candidates.size()];
        double totalWeight = 0;
        for (int i = 0; i < candidates.size(); i++) {
            boolean isInterest = interests.contains(candidates.get(i).getCategory().getSlug());
            weights[i] = isInterest ? INTEREST_WEIGHT : DEFAULT_WEIGHT;
            totalWeight += weights[i];
        }

        double roll = ThreadLocalRandom.current().nextDouble(totalWeight);
        double cumulative = 0;
        for (int i = 0; i < candidates.size(); i++) {
            cumulative += weights[i];
            if (roll < cumulative) {
                return candidates.get(i);
            }
        }
        return candidates.get(candidates.size() - 1); // floating-point safety net
    }

    private AssignmentResult toResult(UUID userId, DailyAssignment assignment, LocalDate today) {
        boolean practicedToday = practiceRepository.findByUserIdAndPracticeDate(userId, today).isPresent();
        boolean replacementAvailable = !assignment.isReplacementUsed() && !practicedToday;
        return new AssignmentResult(assignment, replacementAvailable);
    }
}
