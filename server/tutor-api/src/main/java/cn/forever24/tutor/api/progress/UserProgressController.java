package cn.forever24.tutor.api.progress;

import cn.forever24.tutor.api.auth.CurrentUserKeyResolver;
import cn.forever24.tutor.application.onboarding.OnboardingApplicationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me")
public class UserProgressController {

    private final OnboardingApplicationService onboardingApplicationService;
    private final CurrentUserKeyResolver currentUserKeyResolver;

    public UserProgressController(OnboardingApplicationService onboardingApplicationService, CurrentUserKeyResolver currentUserKeyResolver) {
        this.onboardingApplicationService = onboardingApplicationService;
        this.currentUserKeyResolver = currentUserKeyResolver;
    }

    @GetMapping("/progress")
    public UserLearningProgressResponse getProgress() {
        return UserLearningProgressResponse.from(
                onboardingApplicationService.getLearningProgress(currentUserKeyResolver.resolve()));
    }
}
