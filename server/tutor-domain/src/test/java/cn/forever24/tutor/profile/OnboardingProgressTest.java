package cn.forever24.tutor.profile;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OnboardingProgressTest {

    @Test
    void recoversGoalWhenProfileOrPrimaryGoalIsMissing() {
        assertEquals(OnboardingStep.GOAL, OnboardingProgress.recover(false, false, null, null).step());
        assertEquals(OnboardingStep.GOAL, OnboardingProgress.recover(true, false, "PREFERENCES", null).step());
    }

    @Test
    void recoversNextStepForSavedPrimaryGoal() {
        OnboardingProgress progress = OnboardingProgress.recover(true, true, "GOAL", null);

        assertEquals(OnboardingStep.PREFERENCES, progress.step());
        assertFalse(progress.completed());
    }

    @Test
    void mapsFutureStoredStatusesAndCompletionFlag() {
        assertEquals(OnboardingStep.SELF_ASSESSMENT,
                OnboardingProgress.recover(true, true, "SELF_ASSESSMENT", null).step());
        assertEquals("assessment-1",
                OnboardingProgress.recover(true, true, "ASSESSMENT", "assessment-1").assessmentId());
        assertTrue(OnboardingProgress.recover(true, true, "COMPLETE", "assessment-1").completed());
    }

    @Test
    void unsupportedPersistedStatusFallsBackToPreferences() {
        OnboardingProgress progress = OnboardingProgress.recover(true, true, "BROKEN", "assessment-1");

        assertEquals(OnboardingStep.PREFERENCES, progress.step());
        assertNull(progress.assessmentId());
    }
}
