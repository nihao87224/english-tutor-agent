package cn.forever24.tutor.api.profile;

import cn.forever24.tutor.api.auth.CurrentUserKeyResolver;
import cn.forever24.tutor.application.onboarding.OnboardingApplicationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ProfileController {

    private final OnboardingApplicationService onboardingApplicationService;
    private final CurrentUserKeyResolver currentUserKeyResolver;

    public ProfileController(
            OnboardingApplicationService onboardingApplicationService,
            CurrentUserKeyResolver currentUserKeyResolver
    ) {
        this.onboardingApplicationService = onboardingApplicationService;
        this.currentUserKeyResolver = currentUserKeyResolver;
    }

    @PutMapping("/profile/primary-goal")
    public ProfileSummaryResponse putPrimaryGoal(
            @RequestBody PrimaryGoalRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException("request body is required");
        }
        return ProfileSummaryResponse.from(onboardingApplicationService.savePrimaryGoal(
                currentUserKeyResolver.resolve(),
                request.goal()));
    }

    @PutMapping("/profile/preferences")
    public ProfileSummaryResponse putPreferences(
            @RequestBody PreferenceRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException("request body is required");
        }
        return ProfileSummaryResponse.from(onboardingApplicationService.savePreferences(
                currentUserKeyResolver.resolve(),
                request.dailyMinutes(),
                request.correctionStyle(),
                request.reminderEnabled(),
                request.saveRawText(),
                request.saveRawAudio()));
    }

    @GetMapping("/onboarding/progress")
    public OnboardingProgressResponse getOnboardingProgress() {
        return OnboardingProgressResponse.from(onboardingApplicationService.getProgress(currentUserKeyResolver.resolve()));
    }

    @GetMapping("/settings/privacy")
    public PrivacySettingsResponse getPrivacySettings() {
        return PrivacySettingsResponse.from(onboardingApplicationService.getPrivacySettings(currentUserKeyResolver.resolve()));
    }

    @PutMapping("/settings/privacy")
    public PrivacySettingsResponse putPrivacySettings(
            @RequestBody PrivacySettingsRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException("request body is required");
        }
        return PrivacySettingsResponse.from(onboardingApplicationService.savePrivacySettings(
                currentUserKeyResolver.resolve(),
                request.saveRawText(),
                request.saveRawAudio(),
                request.rawAudioRetentionDays()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ProblemDetail> handleBadRequest(IllegalArgumentException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setTitle("Invalid onboarding request");
        return ResponseEntity.badRequest().body(problem);
    }
}
