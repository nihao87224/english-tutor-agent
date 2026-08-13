package cn.forever24.tutor.profile;

public record OnboardingProgress(
        OnboardingStep step,
        boolean completed,
        String assessmentId
) {

    public OnboardingProgress {
        if (step == null) {
            throw new IllegalArgumentException("onboarding step is required");
        }
    }

    public static OnboardingProgress recover(
            boolean profileExists,
            boolean primaryGoalSaved,
            String persistedStatus,
            String assessmentId
    ) {
        if (!profileExists || !primaryGoalSaved) {
            return new OnboardingProgress(OnboardingStep.GOAL, false, null);
        }
        OnboardingStep recoveredStep = recoverStep(persistedStatus);
        return new OnboardingProgress(
                recoveredStep,
                recoveredStep == OnboardingStep.COMPLETE,
                recoveredStep == OnboardingStep.ASSESSMENT ? assessmentId : null);
    }

    private static OnboardingStep recoverStep(String persistedStatus) {
        if (persistedStatus == null || persistedStatus.isBlank()) {
            return OnboardingStep.PREFERENCES;
        }
        try {
            OnboardingStep step = OnboardingStep.valueOf(persistedStatus);
            if (step == OnboardingStep.GOAL) {
                return OnboardingStep.PREFERENCES;
            }
            return step;
        } catch (IllegalArgumentException exception) {
            return OnboardingStep.PREFERENCES;
        }
    }
}
