package cn.forever24.tutor.api.progress;

import cn.forever24.tutor.profile.UserLearningProgress;

public record UserLearningProgressResponse(String nextStep, String onboardingStep) {

    public static UserLearningProgressResponse from(UserLearningProgress progress) {
        return new UserLearningProgressResponse(progress.nextStep().name(), progress.onboardingStep().name());
    }
}
