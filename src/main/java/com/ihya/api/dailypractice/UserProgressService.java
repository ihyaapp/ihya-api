package com.ihya.api.dailypractice;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.UUID;

@Service
public class UserProgressService {

    private final UserProgressRepository userProgressRepository;

    public UserProgressService(UserProgressRepository userProgressRepository) {
        this.userProgressRepository = userProgressRepository;
    }

    public void createDefaults(UUID userId) {
        userProgressRepository.save(new UserProgress(userId));
    }

    /**
     * A progress row is guaranteed to exist for every user — {@link com.ihya.api.identity.UserService#register}
     * creates it in the same transaction as the user row, with no lazy-create path.
     * A miss here means that invariant was violated, so it's an unchecked failure
     * (logged 500 via the shared catch-all), not a typed domain exception. Mirrors
     * {@code NotificationPreferencesService#getPreferences}.
     */
    public UserProgress getProgress(UUID userId) {
        return userProgressRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("No user_progress row for user " + userId));
    }

    public ProgressUpdate recordPractice(UUID userId, LocalDate practiceDate) {
        UserProgress progress = getProgress(userId);
        int previousLongestStreak = progress.getLongestStreak();
        int previousTotalPracticed = progress.getTotalPracticed();
        progress.applyPractice(practiceDate);
        userProgressRepository.save(progress);
        return new ProgressUpdate(progress, previousLongestStreak, previousTotalPracticed);
    }

    public void deleteForUser(UUID userId) {
        userProgressRepository.deleteById(userId);
    }
}
