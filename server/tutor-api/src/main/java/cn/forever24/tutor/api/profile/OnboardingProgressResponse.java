package cn.forever24.tutor.api.profile;

import cn.forever24.tutor.profile.OnboardingProgress;

public record OnboardingProgressResponse(
        String step,
        boolean completed,
        String assessmentId
) {

    public static OnboardingProgressResponse from(OnboardingProgress progress) {
        return new OnboardingProgressResponse(
                progress.step().name(),
                progress.completed(),
                progress.assessmentId());
    }
}
