package cn.forever24.tutor.application.planning;

import cn.forever24.tutor.application.onboarding.UserProfileRepository;
import cn.forever24.tutor.planning.LearningPlan;
import cn.forever24.tutor.profile.OnboardingProgress;
import cn.forever24.tutor.profile.OnboardingStep;
import cn.forever24.tutor.profile.UserKey;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;

public class LearningPlanApplicationService {

    private final UserProfileRepository userProfileRepository;
    private final LearningPlanRepository learningPlanRepository;
    private final Clock clock;

    public LearningPlanApplicationService(
            UserProfileRepository userProfileRepository,
            LearningPlanRepository learningPlanRepository,
            Clock clock
    ) {
        this.userProfileRepository = userProfileRepository;
        this.learningPlanRepository = learningPlanRepository;
        this.clock = clock;
    }

    public LearningPlan getTodayPlan(String userKeyValue) {
        UserKey userKey = new UserKey(userKeyValue);
        OnboardingProgress progress = userProfileRepository.getOnboardingProgress(userKey);
        if (progress.step().ordinal() < OnboardingStep.RESULT.ordinal()) {
            throw new IllegalArgumentException("initial assessment result is required before planning");
        }
        return learningPlanRepository.getOrGenerateTodayPlan(userKey, LocalDate.now(clock.withZone(ZoneOffset.UTC)));
    }
}
