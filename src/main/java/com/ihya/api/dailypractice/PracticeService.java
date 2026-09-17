package com.ihya.api.dailypractice;

import com.ihya.api.catalogue.SunnahService;
import com.ihya.api.identity.UserNotFoundException;
import com.ihya.api.identity.UserRepository;
import com.ihya.api.notification.NotificationService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * Records practice completions and reports progress
 * (docs/api-contract.md §2 "Daily practice" / "Streak").
 *
 * <p>Same reason as {@link AssignmentService}: depends on {@link UserRepository}
 * directly, not {@code UserService}, to avoid a circular bean dependency with
 * {@code UserService.deleteMe} (which depends on this service).
 */
@Service
public class PracticeService {

    private final PracticeRepository practiceRepository;
    private final UserProgressService userProgressService;
    private final SunnahService sunnahService;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public PracticeService(PracticeRepository practiceRepository,
                            UserProgressService userProgressService,
                            SunnahService sunnahService,
                            UserRepository userRepository,
                            NotificationService notificationService) {
        this.practiceRepository = practiceRepository;
        this.userProgressService = userProgressService;
        this.sunnahService = sunnahService;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    /**
     * One practice per local day, enforced by {@code UNIQUE(user_id, practice_date)}.
     *
     * <p>Uses {@code INSERT ... ON CONFLICT DO NOTHING} ({@link
     * PracticeRepository#insertIgnoringConflict}) rather than the codebase's
     * usual persist-and-remap-a-thrown-exception pattern: a same-day conflict
     * here needs a <em>follow-up read</em> in the same transaction (the
     * existing practice + current progress), and a Postgres transaction
     * refuses any further command once one statement in it has thrown —
     * catching {@code DataIntegrityViolationException} and querying again
     * right after doesn't work, and Spring additionally marks the whole
     * ambient transaction rollback-only the moment that exception is thrown,
     * so even isolating the retry in its own transaction still leaves the
     * outer one doomed. {@code ON CONFLICT DO NOTHING} sidesteps this
     * entirely: Postgres reports "0 rows inserted" as an ordinary result, not
     * an exception, so the same transaction stays healthy for whatever comes
     * next.
     */
    @Transactional
    public PracticeRecordResult recordPractice(UUID userId, UUID sunnahId, String feeling) {
        sunnahService.getById(sunnahId); // 404s via SunnahNotFoundException if the catalogue has no such Sunnah
        LocalDate today = resolveLocalDate(userId);

        int rowsInserted = practiceRepository.insertIgnoringConflict(userId, sunnahId, today, feeling);
        Practice practice = practiceRepository.findByUserIdAndPracticeDate(userId, today)
                .orElseThrow(() -> new IllegalStateException(
                        "practices row missing immediately after insert for user " + userId));

        if (rowsInserted == 0) {
            // Someone else's write already claimed today -- not an error, the
            // contract's own conflict case (existing practice + current progress).
            return PracticeRecordResult.alreadyExisted(practice, userProgressService.getProgress(userId));
        }

        ProgressUpdate update = userProgressService.recordPractice(userId, today);
        String milestoneUnlocked = MilestoneEvaluator.newlyCrossed(
                update.previousLongestStreak(), update.previousTotalPracticed(),
                update.progress().getLongestStreak(), update.progress().getTotalPracticed());

        if (milestoneUnlocked != null) {
            // Same transaction as the practice write (docs/api-contract.md §3
            // step 9) -- no scheduler needed for this notification type.
            notificationService.recordMilestoneEarned(userId, MilestoneEvaluator.titleFor(milestoneUnlocked));
        }

        return PracticeRecordResult.created(practice, update.progress(), milestoneUnlocked);
    }

    /** Feeling only — never re-triggers streak math (docs/api-contract.md §2). */
    @Transactional
    public Practice updateFeeling(UUID userId, UUID practiceId, String feeling) {
        Practice practice = practiceRepository.findById(practiceId)
                .filter(p -> p.getUserId().equals(userId))
                .orElseThrow(() -> new PracticeNotFoundException(practiceId));
        practice.setFeeling(feeling);
        return practiceRepository.save(practice);
    }

    public PracticePage listPractices(UUID userId, String cursor, int limit) {
        LocalDate cursorDate = cursor == null ? null : decodeCursor(cursor);
        List<Practice> rows = cursorDate == null
                ? practiceRepository.findByUserIdOrderByPracticeDateDesc(userId, PageRequest.of(0, limit + 1))
                : practiceRepository.findByUserIdAndPracticeDateLessThanOrderByPracticeDateDesc(
                        userId, cursorDate, PageRequest.of(0, limit + 1));

        boolean hasMore = rows.size() > limit;
        List<Practice> page = hasMore ? rows.subList(0, limit) : rows;
        String nextCursor = hasMore ? encodeCursor(page.get(page.size() - 1).getPracticeDate()) : null;
        return new PracticePage(page, nextCursor);
    }

    public UserProgress getProgress(UUID userId) {
        return userProgressService.getProgress(userId);
    }

    public void deleteAllForUser(UUID userId) {
        practiceRepository.deleteAllByUserId(userId);
    }

    private LocalDate resolveLocalDate(UUID userId) {
        String timezone = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new)
                .getTimezone();
        return LocalDate.now(ZoneId.of(timezone));
    }

    private static String encodeCursor(LocalDate date) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(date.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static LocalDate decodeCursor(String cursor) {
        try {
            return LocalDate.parse(new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8));
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Invalid cursor", ex);
        }
    }
}
