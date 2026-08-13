package cn.forever24.tutor.application.onboarding;

import cn.forever24.tutor.profile.CorrectionStyle;
import cn.forever24.tutor.profile.LearningPreferences;
import cn.forever24.tutor.profile.OnboardingProgress;
import cn.forever24.tutor.profile.OnboardingStep;
import cn.forever24.tutor.profile.PrimaryGoal;
import cn.forever24.tutor.profile.PrivacySettings;
import cn.forever24.tutor.profile.ProfileSummary;
import cn.forever24.tutor.profile.RawContentRetention;
import cn.forever24.tutor.profile.UserKey;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OnboardingApplicationServiceTest {

    private final FakeUserProfileRepository repository = new FakeUserProfileRepository();
    private final OnboardingApplicationService service = new OnboardingApplicationService(repository);

    @Test
    void savesPrimaryGoalAndMovesProgressToPreferences() {
        ProfileSummary summary = service.savePrimaryGoal("user-1", "WORKPLACE");

        assertEquals(PrimaryGoal.WORKPLACE, summary.primaryGoal());
        assertEquals(20, summary.dailyMinutes());
        assertEquals(OnboardingStep.PREFERENCES, service.getProgress("user-1").step());
    }

    @Test
    void replacingPrimaryGoalIncrementsProfileVersion() {
        service.savePrimaryGoal("user-1", "WORKPLACE");

        ProfileSummary summary = service.savePrimaryGoal("user-1", "IELTS");

        assertEquals(PrimaryGoal.IELTS, summary.primaryGoal());
        assertEquals(2, summary.profileVersion());
    }

    @Test
    void rejectsInvalidGoalBeforeOverwritingExistingState() {
        service.savePrimaryGoal("user-1", "GENERAL");

        assertThrows(IllegalArgumentException.class, () -> service.savePrimaryGoal("user-1", "TRAVEL"));

        assertEquals(PrimaryGoal.GENERAL, repository.profiles.get(new UserKey("user-1")).primaryGoal());
    }

    @Test
    void savesPreferencesAndMovesProgressToSelfAssessment() {
        service.savePrimaryGoal("user-1", "WORKPLACE");

        ProfileSummary summary = service.savePreferences("user-1", 30, "LIGHT", true, false, true);

        assertEquals(30, summary.dailyMinutes());
        assertEquals(CorrectionStyle.LIGHT, summary.correctionStyle());
        assertEquals(RawContentRetention.PROCESS_ONLY, summary.rawTextRetention());
        assertEquals(OnboardingStep.SELF_ASSESSMENT, service.getProgress("user-1").step());
    }

    @Test
    void rejectsInvalidPreferencesBeforeOverwritingExistingState() {
        service.savePrimaryGoal("user-1", "GENERAL");
        service.savePreferences("user-1", 20, "STANDARD", false, true, true);

        assertThrows(IllegalArgumentException.class,
                () -> service.savePreferences("user-1", 17, "STRICT", true, false, false));

        assertEquals(20, repository.profiles.get(new UserKey("user-1")).dailyMinutes());
        assertEquals(CorrectionStyle.STANDARD, repository.profiles.get(new UserKey("user-1")).correctionStyle());
    }

    @Test
    void rejectsPreferencesBeforePrimaryGoal() {
        assertThrows(IllegalArgumentException.class,
                () -> service.savePreferences("user-1", 20, "STANDARD", false, true, true));
    }

    private static final class FakeUserProfileRepository implements UserProfileRepository {

        private final Map<UserKey, ProfileSummary> profiles = new HashMap<>();
        private final Map<UserKey, OnboardingStep> steps = new HashMap<>();

        @Override
        public ProfileSummary savePrimaryGoal(UserKey userKey, PrimaryGoal primaryGoal) {
            ProfileSummary existing = profiles.get(userKey);
            long version = existing == null || existing.primaryGoal() == primaryGoal
                    ? 1
                    : existing.profileVersion() + 1;
            ProfileSummary summary = new ProfileSummary(
                    primaryGoal,
                    20,
                    CorrectionStyle.STANDARD,
                    false,
                    RawContentRetention.STORE,
                    RawContentRetention.STORE,
                    0,
                    false,
                    version);
            profiles.put(userKey, summary);
            steps.put(userKey, OnboardingStep.PREFERENCES);
            return summary;
        }

        @Override
        public ProfileSummary savePreferences(UserKey userKey, LearningPreferences preferences) {
            ProfileSummary existing = profiles.get(userKey);
            if (existing == null) {
                throw new IllegalArgumentException("primary goal must be saved before preferences");
            }
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
}
