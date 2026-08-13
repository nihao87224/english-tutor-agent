package cn.forever24.tutor.infrastructure.profile;

import cn.forever24.tutor.profile.CorrectionStyle;
import cn.forever24.tutor.profile.LearningPreferences;
import cn.forever24.tutor.profile.OnboardingStep;
import cn.forever24.tutor.profile.PrimaryGoal;
import cn.forever24.tutor.profile.PrivacySettings;
import cn.forever24.tutor.profile.ProfileSummary;
import cn.forever24.tutor.profile.RawContentRetention;
import cn.forever24.tutor.profile.UserKey;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InMemoryUserProfileRepositoryTest {

    private final InMemoryUserProfileRepository repository = new InMemoryUserProfileRepository();

    @Test
    void restoresGoalProgressAfterSave() {
        UserKey userKey = new UserKey("local-user");

        repository.savePrimaryGoal(userKey, PrimaryGoal.WORKPLACE);

        assertEquals(OnboardingStep.PREFERENCES, repository.getOnboardingProgress(userKey).step());
    }

    @Test
    void replacingGoalIncrementsVersionButRepeatingSameGoalDoesNot() {
        UserKey userKey = new UserKey("local-user");

        repository.savePrimaryGoal(userKey, PrimaryGoal.WORKPLACE);
        ProfileSummary repeated = repository.savePrimaryGoal(userKey, PrimaryGoal.WORKPLACE);
        ProfileSummary replaced = repository.savePrimaryGoal(userKey, PrimaryGoal.IELTS);

        assertEquals(1, repeated.profileVersion());
        assertEquals(2, replaced.profileVersion());
    }

    @Test
    void savingPreferencesMovesProgressAndIncrementsWhenValuesChange() {
        UserKey userKey = new UserKey("local-user");

        repository.savePrimaryGoal(userKey, PrimaryGoal.WORKPLACE);
        ProfileSummary summary = repository.savePreferences(userKey, new LearningPreferences(
                30,
                CorrectionStyle.LIGHT,
                true,
                RawContentRetention.PROCESS_ONLY,
                RawContentRetention.STORE));

        assertEquals(2, summary.profileVersion());
        assertEquals(30, summary.dailyMinutes());
        assertEquals(OnboardingStep.SELF_ASSESSMENT, repository.getOnboardingProgress(userKey).step());
    }

    @Test
    void privacySettingsDoNotChangeLearningPreferences() {
        UserKey userKey = new UserKey("local-user");

        repository.savePrimaryGoal(userKey, PrimaryGoal.GENERAL);
        repository.savePreferences(userKey, new LearningPreferences(
                45,
                CorrectionStyle.STRICT,
                true,
                RawContentRetention.STORE,
                RawContentRetention.STORE));

        PrivacySettings settings = repository.savePrivacySettings(userKey, new PrivacySettings(
                RawContentRetention.PROCESS_ONLY,
                RawContentRetention.PROCESS_ONLY,
                7));

        assertEquals(false, settings.saveRawText());
        assertEquals(false, repository.getPrivacySettings(userKey).saveRawAudio());
        ProfileSummary summary = repository.savePreferences(userKey, new LearningPreferences(
                45,
                CorrectionStyle.STRICT,
                true,
                RawContentRetention.PROCESS_ONLY,
                RawContentRetention.PROCESS_ONLY));
        assertEquals(45, summary.dailyMinutes());
        assertEquals(CorrectionStyle.STRICT, summary.correctionStyle());
    }
}
