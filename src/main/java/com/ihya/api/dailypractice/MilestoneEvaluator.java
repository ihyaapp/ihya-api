package com.ihya.api.dailypractice;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The 5 concrete milestones evaluated server-side for v1
 * (docs/api-contract.md §2 "Streak" — the 6 {@code special} milestones are
 * client-owned and not evaluated here).
 *
 * <p>Keys and titles are copied verbatim from
 * {@code ihya-mobile/src/constants/milestones.ts}'s {@code MILESTONES} array
 * — the client looks up a server-reported key with {@code MILESTONES.find},
 * so these must match byte for byte, not just carry the same meaning.
 *
 * <p>A milestone is "earned" against the user's all-time high
 * ({@code longestStreak} / {@code totalPracticed}), never the live
 * {@code streak} — reaching a 7-day streak once unlocks it permanently, even
 * after the streak later resets to 0.
 */
final class MilestoneEvaluator {

    private static final Map<Integer, String> STREAK_MILESTONES = new LinkedHashMap<>();
    private static final Map<Integer, String> TOTAL_MILESTONES = new LinkedHashMap<>();
    private static final Map<String, String> TITLES = new LinkedHashMap<>();

    static {
        STREAK_MILESTONES.put(3, "3-day-streak");
        STREAK_MILESTONES.put(7, "7-day-streak");
        STREAK_MILESTONES.put(30, "30-day-streak");
        TOTAL_MILESTONES.put(25, "25-practiced");
        TOTAL_MILESTONES.put(100, "100-practiced");

        TITLES.put("3-day-streak", "3 day streak");
        TITLES.put("7-day-streak", "7 day streak");
        TITLES.put("30-day-streak", "30 day streak");
        TITLES.put("25-practiced", "25 Sunnahs revived");
        TITLES.put("100-practiced", "100 Sunnahs revived");
    }

    private MilestoneEvaluator() {
    }

    /** The mobile-facing display title for a milestone key, e.g. {@code "3-day-streak"} &rarr; {@code "3 day streak"}. */
    static String titleFor(String milestoneKey) {
        return TITLES.get(milestoneKey);
    }

    /**
     * The single milestone key newly crossed by this write, or {@code null}.
     * Streak thresholds are checked before total thresholds when a write
     * happens to cross one of each at once (e.g. totalPracticed 24→25 and
     * streak 6→7 in the same practice) — {@code PracticeResult.milestoneUnlocked}
     * only has room for one, and streak is the more immediate, visible signal.
     */
    static String newlyCrossed(int previousLongestStreak, int previousTotalPracticed,
                                int longestStreak, int totalPracticed) {
        for (Map.Entry<Integer, String> entry : STREAK_MILESTONES.entrySet()) {
            int threshold = entry.getKey();
            if (previousLongestStreak < threshold && longestStreak >= threshold) {
                return entry.getValue();
            }
        }
        for (Map.Entry<Integer, String> entry : TOTAL_MILESTONES.entrySet()) {
            int threshold = entry.getKey();
            if (previousTotalPracticed < threshold && totalPracticed >= threshold) {
                return entry.getValue();
            }
        }
        return null;
    }

    /** Every milestone key earned so far, for {@code Progress.earnedMilestoneKeys}. */
    static List<String> allEarned(int longestStreak, int totalPracticed) {
        List<String> earned = new ArrayList<>();
        STREAK_MILESTONES.forEach((threshold, key) -> {
            if (longestStreak >= threshold) {
                earned.add(key);
            }
        });
        TOTAL_MILESTONES.forEach((threshold, key) -> {
            if (totalPracticed >= threshold) {
                earned.add(key);
            }
        });
        return earned;
    }
}
