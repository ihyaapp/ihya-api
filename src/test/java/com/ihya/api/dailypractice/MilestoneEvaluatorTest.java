package com.ihya.api.dailypractice;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Pure unit tests for {@link MilestoneEvaluator}. No mocks needed — static logic only. */
class MilestoneEvaluatorTest {

    @Test
    void newlyCrossed_streakThresholdJustReached_returnsItsKey() {
        assertThat(MilestoneEvaluator.newlyCrossed(2, 10, 3, 11)).isEqualTo("3-day-streak");
        assertThat(MilestoneEvaluator.newlyCrossed(6, 10, 7, 11)).isEqualTo("7-day-streak");
        assertThat(MilestoneEvaluator.newlyCrossed(29, 10, 30, 11)).isEqualTo("30-day-streak");
    }

    @Test
    void newlyCrossed_totalThresholdJustReached_returnsItsKey() {
        assertThat(MilestoneEvaluator.newlyCrossed(1, 24, 1, 25)).isEqualTo("25-practiced");
        assertThat(MilestoneEvaluator.newlyCrossed(1, 99, 1, 100)).isEqualTo("100-practiced");
    }

    @Test
    void newlyCrossed_noThresholdCrossed_returnsNull() {
        assertThat(MilestoneEvaluator.newlyCrossed(4, 10, 5, 11)).isNull();
    }

    @Test
    void newlyCrossed_alreadyPastThreshold_returnsNullOnLaterWrites() {
        // longestStreak was already 3 before this write (e.g. streak reset then
        // grew again) -- 3-day-streak was earned on an earlier write, not this one.
        assertThat(MilestoneEvaluator.newlyCrossed(3, 10, 4, 11)).isNull();
    }

    @Test
    void newlyCrossed_streakAndTotalCrossedInSameWrite_streakTakesPriority() {
        // longestStreak 6->7 and totalPracticed 24->25 in the same write.
        assertThat(MilestoneEvaluator.newlyCrossed(6, 24, 7, 25)).isEqualTo("7-day-streak");
    }

    @Test
    void allEarned_belowEveryThreshold_returnsEmpty() {
        assertThat(MilestoneEvaluator.allEarned(2, 10)).isEmpty();
    }

    @Test
    void allEarned_pastSomeThresholds_returnsExactlyThoseKeysInOrder() {
        assertThat(MilestoneEvaluator.allEarned(10, 30))
                .containsExactly("3-day-streak", "7-day-streak", "25-practiced");
    }

    @Test
    void allEarned_pastEveryThreshold_returnsAllFiveKeys() {
        assertThat(MilestoneEvaluator.allEarned(30, 100))
                .containsExactly("3-day-streak", "7-day-streak", "30-day-streak", "25-practiced", "100-practiced");
    }

    // ----------------------------------------------------------------------
    // titleFor()
    // ----------------------------------------------------------------------

    @Test
    void titleFor_everyConcreteKey_returnsMobileFacingTitle() {
        assertThat(MilestoneEvaluator.titleFor("3-day-streak")).isEqualTo("3 day streak");
        assertThat(MilestoneEvaluator.titleFor("7-day-streak")).isEqualTo("7 day streak");
        assertThat(MilestoneEvaluator.titleFor("30-day-streak")).isEqualTo("30 day streak");
        assertThat(MilestoneEvaluator.titleFor("25-practiced")).isEqualTo("25 Sunnahs revived");
        assertThat(MilestoneEvaluator.titleFor("100-practiced")).isEqualTo("100 Sunnahs revived");
    }

    @Test
    void titleFor_unknownKey_returnsNull() {
        assertThat(MilestoneEvaluator.titleFor("not-a-real-key")).isNull();
    }
}
