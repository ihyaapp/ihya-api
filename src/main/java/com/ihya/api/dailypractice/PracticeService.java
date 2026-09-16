package com.ihya.api.dailypractice;

import com.ihya.api.catalogue.SunnahService;
import com.ihya.api.identity.UserNotFoundException;
import com.ihya.api.identity.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
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

    public PracticeService(PracticeRepository practiceRepository,
                            UserProgressService userProgressService,
                            SunnahService sunnahService,
                            UserRepository userRepository) {
        this.practiceRepository = practiceRepository;
        this.userProgressService = userProgressService;
        this.sunnahService = sunnahService;
        this.userRepository = userRepository;
    }

    /**
     * One practice per local day, enforced by {@code UNIQUE(user_id, practice_date)}
     * — persist-and-remap, not check-then-insert (CLAUDE.md's uniqueness
     * pattern), same as {@code UserService.register} on email. The conflict
     * case isn't an error: it returns the existing state via
     * {@link PracticeRecordResult#alreadyExisted}, matching how
     * {@code PushTokenService} treats a duplicate registration as a normal
     * outcome rather than a thrown exception.
     */
    @Transactional
    public PracticeRecordResult recordPractice(UUID userId, UUID sunnahId, String feeling) {
        sunnahService.getById(sunnahId); // 404s via SunnahNotFoundException if the catalogue has no such Sunnah
        LocalDate today = resolveLocalDate(userId);

        Practice practice = new Practice(userId, sunnahId, today, feeling);
        try {
            practice = practiceRepository.saveAndFlush(practice);
        } catch (DataIntegrityViolationException ex) {
            if (!isPracticeDateUniqueViolation(ex)) {
                throw ex;
            }
            Practice existing = practiceRepository.findByUserIdAndPracticeDate(userId, today)
                    .orElseThrow(() -> ex);
            return PracticeRecordResult.alreadyExisted(existing, userProgressService.getProgress(userId));
        }

        ProgressUpdate update = userProgressService.recordPractice(userId, today);
        String milestoneUnlocked = MilestoneEvaluator.newlyCrossed(
                update.previousLongestStreak(), update.previousTotalPracticed(),
                update.progress().getLongestStreak(), update.progress().getTotalPracticed());

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

    private static boolean isPracticeDateUniqueViolation(DataIntegrityViolationException ex) {
        Throwable cause = ex.getMostSpecificCause();
        String message = cause == null ? null : cause.getMessage();
        return message != null
                && message.toLowerCase(Locale.ROOT).contains("practices_user_id_practice_date_key");
    }
}
