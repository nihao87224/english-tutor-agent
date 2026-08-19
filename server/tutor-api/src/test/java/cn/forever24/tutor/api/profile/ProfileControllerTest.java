package cn.forever24.tutor.api.profile;

import cn.forever24.tutor.api.auth.CurrentUserKeyResolver;
import cn.forever24.tutor.application.assessment.AssessmentResultRepository;
import cn.forever24.tutor.application.onboarding.OnboardingApplicationService;
import cn.forever24.tutor.application.onboarding.UserProfileRepository;
import cn.forever24.tutor.assessment.AssessmentResult;
import cn.forever24.tutor.profile.CorrectionStyle;
import cn.forever24.tutor.profile.LearningPreferences;
import cn.forever24.tutor.profile.OnboardingProgress;
import cn.forever24.tutor.profile.OnboardingStep;
import cn.forever24.tutor.profile.PrimaryGoal;
import cn.forever24.tutor.profile.PrivacySettings;
import cn.forever24.tutor.profile.ProfileSummary;
import cn.forever24.tutor.profile.RawContentRetention;
import cn.forever24.tutor.profile.UserKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ProfileControllerTest {

    private final ProfileController controller = new ProfileController(
            new OnboardingApplicationService(new FakeUserProfileRepository(), new EmptyAssessmentResultRepository()),
            new CurrentUserKeyResolver(ignored -> "user-1"));

    @BeforeEach
    void authenticate() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("1", null, "ROLE_USER"));
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void savesPrimaryGoalAndRestoresProgress() {
        ProfileSummaryResponse summary = controller.putPrimaryGoal(
                new PrimaryGoalRequest("WORKPLACE"));

        OnboardingProgressResponse progress = controller.getOnboardingProgress();

        assertEquals("WORKPLACE", summary.primaryGoal());
        assertEquals(20, summary.dailyMinutes());
        assertEquals("PREFERENCES", progress.step());
        assertFalse(progress.completed());
    }

    @Test
    void invalidGoalMapsToBadRequestProblem() {
        ResponseEntity<?> response = controller.handleBadRequest(new IllegalArgumentException("unsupported primary goal"));

        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void savesPreferencesAndReturnsProfileSummary() {
        controller.putPrimaryGoal(new PrimaryGoalRequest("WORKPLACE"));

        ProfileSummaryResponse summary = controller.putPreferences(
                new PreferenceRequest(30, "LIGHT", true, false, true));
        OnboardingProgressResponse progress = controller.getOnboardingProgress();

        assertEquals(30, summary.dailyMinutes());
        assertEquals("LIGHT", summary.correctionStyle());
        assertEquals("SELF_ASSESSMENT", progress.step());
    }

    @Test
    void readsAndUpdatesPrivacySettings() {
        controller.putPrimaryGoal(new PrimaryGoalRequest("GENERAL"));

        PrivacySettingsResponse defaults = controller.getPrivacySettings();
        PrivacySettingsResponse updated = controller.putPrivacySettings(
                new PrivacySettingsRequest(false, false, 7));

        assertEquals(true, defaults.saveRawText());
        assertEquals(false, updated.saveRawAudio());
        assertEquals(7, updated.rawAudioRetentionDays());
    }

    private static final class FakeUserProfileRepository implements UserProfileRepository {

        private final Map<UserKey, ProfileSummary> profiles = new HashMap<>();
        private final Map<UserKey, OnboardingStep> steps = new HashMap<>();

        @Override
        public ProfileSummary savePrimaryGoal(UserKey userKey, PrimaryGoal primaryGoal) {
            ProfileSummary summary = new ProfileSummary(
                    primaryGoal,
                    20,
                    CorrectionStyle.STANDARD,
                    false,
                    RawContentRetention.STORE,
                    RawContentRetention.STORE,
                    0,
                    false,
                    1);
            profiles.put(userKey, summary);
            steps.put(userKey, OnboardingStep.PREFERENCES);
            return summary;
        }

        @Override
        public ProfileSummary savePreferences(UserKey userKey, LearningPreferences preferences) {
            ProfileSummary existing = profiles.get(userKey);
            ProfileSummary summary = new ProfileSummary(
                    existing.primaryGoal(),
                    preferences.dailyMinutes(),
                    preferences.correctionStyle(),
                    preferences.reminderEnabled(),
                    preferences.rawTextRetention(),
                    preferences.rawAudioRetention(),
                    existing.rawAudioRetentionDays(),
                    false,
                    existing.profileVersion() + 1);
            profiles.put(userKey, summary);
            steps.put(userKey, OnboardingStep.SELF_ASSESSMENT);
            return summary;
        }

        @Override
        public PrivacySettings getPrivacySettings(UserKey userKey) {
            ProfileSummary existing = profiles.get(userKey);
            return new PrivacySettings(
                    existing.rawTextRetention(),
                    existing.rawAudioRetention(),
                    existing.rawAudioRetentionDays());
        }

        @Override
        public PrivacySettings savePrivacySettings(UserKey userKey, PrivacySettings privacySettings) {
            ProfileSummary existing = profiles.get(userKey);
            profiles.put(userKey, new ProfileSummary(
                    existing.primaryGoal(),
                    existing.dailyMinutes(),
                    existing.correctionStyle(),
                    existing.reminderEnabled(),
                    privacySettings.rawTextRetention(),
                    privacySettings.rawAudioRetention(),
                    privacySettings.rawAudioRetentionDays(),
                    existing.onboardingCompleted(),
                    existing.profileVersion() + 1));
            return privacySettings;
        }

        @Override
        public void advanceOnboardingToAssessment(UserKey userKey) {
            steps.put(userKey, OnboardingStep.ASSESSMENT);
        }

        @Override
        public void advanceOnboardingToResult(UserKey userKey) {
            steps.put(userKey, OnboardingStep.RESULT);
        }

        @Override
        public OnboardingProgress getOnboardingProgress(UserKey userKey) {
            if (profiles.containsKey(userKey)) {
                return new OnboardingProgress(steps.getOrDefault(userKey, OnboardingStep.PREFERENCES), false, null);
            }
            return new OnboardingProgress(OnboardingStep.GOAL, false, null);
        }
    }

    private static final class EmptyAssessmentResultRepository implements AssessmentResultRepository {

        @Override
        public AssessmentResult completeInitialAssessment(UserKey userKey, String assessmentId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public AssessmentResult getAssessmentResult(UserKey userKey, String assessmentId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean hasCompletedInitialAssessmentResult(UserKey userKey) {
            return false;
        }
    }
}
