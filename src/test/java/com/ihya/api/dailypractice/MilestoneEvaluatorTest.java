package com.ihya.api.dailypractice;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Pure unit tests for {@link MilestoneEvaluator}. No mocks needed — static logic only. */
class MilestoneEvaluatorTest {

    @Test
    void newlyCrossed_streakThresholdJustReached_returnsItsKey() {
        assertThat(MilestoneEvaluator.newlyCrossed(2, 10, 3, 11)).isEqualTo("streak_3");
        assertThat(MilestoneEvaluator.newlyCrossed(6, 10, 7, 11)).isEqualTo("streak_7");
        assertThat(MilestoneEvaluator.newlyCrossed(29, 10, 30, 11)).isEqualTo("streak_30");
    }

    @Test
    void newlyCrossed_totalThresholdJustReached_returnsItsKey() {
        assertThat(MilestoneEvaluator.newlyCrossed(1, 24, 1, 25)).isEqualTo("total_25");
        assertThat(MilestoneEvaluator.newlyCrossed(1, 99, 1, 100)).isEqualTo("total_100");
    }

    @Test
    void newlyCrossed_noThresholdCrossed_returnsNull() {
        assertThat(MilestoneEvaluator.newlyCrossed(4, 10, 5, 11)).isNull();
    }

    @Test
    void newlyCrossed_alreadyPastThreshold_returnsNullOnLaterWrites() {
        // longestStreak was already 3 before this write (e.g. streak reset then
        // grew again) -- streak_3 was earned on an earlier write, not this one.
        assertThat(MilestoneEvaluator.newlyCrossed(3, 10, 4, 11)).isNull();
    }

    @Test
    void newlyCrossed_streakAndTotalCrossedInSameWrite_streakTakesPriority() {
        // longestStreak 6->7 and totalPracticed 24->25 in the same write.
        assertThat(MilestoneEvaluator.newlyCrossed(6, 24, 7, 25)).isEqualTo("streak_7");
    }

    @Test
    void allEarned_belowEveryThreshold_returnsEmpty() {
        assertThat(MilestoneEvaluator.allEarned(2, 10)).isEmpty();
    }

    @Test
    void allEarned_pastSomeThresholds_returnsExactlyThoseKeysInOrder() {
        assertThat(MilestoneEvaluator.allEarned(10, 30))
                .containsExactly("streak_3", "streak_7", "total_25");
    }

    @Test
    void allEarned_pastEveryThreshold_returnsAllFiveKeys() {
        assertThat(MilestoneEvaluator.allEarned(30, 100))
                .containsExactly("streak_3", "streak_7", "streak_30", "total_25", "total_100");
    }
}
