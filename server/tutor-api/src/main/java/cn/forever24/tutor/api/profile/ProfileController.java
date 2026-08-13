package cn.forever24.tutor.api.profile;

import cn.forever24.tutor.application.onboarding.OnboardingApplicationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ProfileController {

    private static final String DEFAULT_USER_KEY = "local-dev-user";

    private final OnboardingApplicationService onboardingApplicationService;

    public ProfileController(OnboardingApplicationService onboardingApplicationService) {
        this.onboardingApplicationService = onboardingApplicationService;
    }

    @PutMapping("/profile/primary-goal")
    public ProfileSummaryResponse putPrimaryGoal(
            @RequestHeader(name = "X-User-Key", required = false) String userKey,
            @RequestBody PrimaryGoalRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException("request body is required");
        }
        return ProfileSummaryResponse.from(onboardingApplicationService.savePrimaryGoal(
                resolveUserKey(userKey),
                request.goal()));
    }

    @PutMapping("/profile/preferences")
    public ProfileSummaryResponse putPreferences(
            @RequestHeader(name = "X-User-Key", required = false) String userKey,
            @RequestBody PreferenceRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException("request body is required");
        }
        return ProfileSummaryResponse.from(onboardingApplicationService.savePreferences(
                resolveUserKey(userKey),
                request.dailyMinutes(),
                request.correctionStyle(),
                request.reminderEnabled(),
                request.saveRawText(),
                request.saveRawAudio()));
    }

    @GetMapping("/onboarding/progress")
    public OnboardingProgressResponse getOnboardingProgress(
            @RequestHeader(name = "X-User-Key", required = false) String userKey
    ) {
        return OnboardingProgressResponse.from(onboardingApplicationService.getProgress(resolveUserKey(userKey)));
    }

    @GetMapping("/settings/privacy")
    public PrivacySettingsResponse getPrivacySettings(
            @RequestHeader(name = "X-User-Key", required = false) String userKey
    ) {
        return PrivacySettingsResponse.from(onboardingApplicationService.getPrivacySettings(resolveUserKey(userKey)));
    }

    @PutMapping("/settings/privacy")
    public PrivacySettingsResponse putPrivacySettings(
            @RequestHeader(name = "X-User-Key", required = false) String userKey,
            @RequestBody PrivacySettingsRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException("request body is required");
        }
        return PrivacySettingsResponse.from(onboardingApplicationService.savePrivacySettings(
                resolveUserKey(userKey),
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

    private String resolveUserKey(String userKey) {
        if (userKey == null || userKey.isBlank()) {
            return DEFAULT_USER_KEY;
        }
        return userKey;
    }
}
