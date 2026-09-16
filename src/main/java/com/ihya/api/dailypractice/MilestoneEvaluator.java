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
 * <p><strong>Keys are placeholders.</strong> They must end up matching
 * {@code ihya-mobile/src/constants/milestones.ts} exactly, byte for byte —
 * that file wasn't available while writing this, so {@code streak_3} /
 * {@code streak_7} / {@code streak_30} / {@code total_25} / {@code total_100}
 * are self-describing stand-ins, not final. Confirm against the mobile repo
 * before this ships.
 *
 * <p>A milestone is "earned" against the user's all-time high
 * ({@code longestStreak} / {@code totalPracticed}), never the live
 * {@code streak} — reaching a 7-day streak once unlocks it permanently, even
 * after the streak later resets to 0.
 */
final class MilestoneEvaluator {

    private static final Map<Integer, String> STREAK_MILESTONES = new LinkedHashMap<>();
    private static final Map<Integer, String> TOTAL_MILESTONES = new LinkedHashMap<>();

    static {
        STREAK_MILESTONES.put(3, "streak_3");
        STREAK_MILESTONES.put(7, "streak_7");
        STREAK_MILESTONES.put(30, "streak_30");
        TOTAL_MILESTONES.put(25, "total_25");
        TOTAL_MILESTONES.put(100, "total_100");
    }

    private MilestoneEvaluator() {
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
