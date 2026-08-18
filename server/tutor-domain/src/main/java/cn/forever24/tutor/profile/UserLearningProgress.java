package cn.forever24.tutor.profile;

public record UserLearningProgress(
        UserLearningNextStep nextStep,
        OnboardingStep onboardingStep
) {
    public UserLearningProgress {
        if (nextStep == null || onboardingStep == null) {
            throw new IllegalArgumentException("learning progress values are required");
        }
    }

    public static UserLearningProgress from(OnboardingProgress onboardingProgress) {
        OnboardingStep step = onboardingProgress.step();
        UserLearningNextStep nextStep = switch (step) {
            case GOAL, PREFERENCES, SELF_ASSESSMENT -> UserLearningNextStep.ONBOARDING_REQUIRED;
            case ASSESSMENT -> UserLearningNextStep.ASSESSMENT_REQUIRED;
            case RESULT, COMPLETE -> UserLearningNextStep.READY_FOR_PLAN;
        };
        return new UserLearningProgress(nextStep, step);
    }
}
