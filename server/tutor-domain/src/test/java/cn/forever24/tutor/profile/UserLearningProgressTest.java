package cn.forever24.tutor.profile;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserLearningProgressTest {

    @Test
    void newUserRequiresOnboarding() {
        assertEquals(UserLearningNextStep.ONBOARDING_REQUIRED,
                UserLearningProgress.from(new OnboardingProgress(OnboardingStep.GOAL, false, null), false).nextStep());
    }

    @Test
    void completedOnboardingRequiresInitialAssessment() {
        assertEquals(UserLearningNextStep.ASSESSMENT_REQUIRED,
                UserLearningProgress.from(new OnboardingProgress(OnboardingStep.ASSESSMENT, false, "assessment-1"), false).nextStep());
    }

    @Test
    void completedAssessmentIsReadyForPlan() {
        assertEquals(UserLearningNextStep.READY_FOR_PLAN,
                UserLearningProgress.from(new OnboardingProgress(OnboardingStep.RESULT, false, null), true).nextStep());
    }

    @Test
    void resultStepWithoutAssessmentResultRequiresAssessment() {
        assertEquals(UserLearningNextStep.ASSESSMENT_REQUIRED,
                UserLearningProgress.from(new OnboardingProgress(OnboardingStep.RESULT, false, null), false).nextStep());
    }

    @Test
    void completedUserIsReadyForPlan() {
        assertEquals(UserLearningNextStep.READY_FOR_PLAN,
                UserLearningProgress.from(new OnboardingProgress(OnboardingStep.COMPLETE, true, null), true).nextStep());
    }
}
