package cn.forever24.tutor.application.onboarding;

import cn.forever24.tutor.profile.LearningPreferences;
import cn.forever24.tutor.profile.OnboardingProgress;
import cn.forever24.tutor.profile.PrimaryGoal;
import cn.forever24.tutor.profile.PrivacySettings;
import cn.forever24.tutor.profile.ProfileSummary;
import cn.forever24.tutor.profile.UserKey;

public class OnboardingApplicationService {

    private final UserProfileRepository userProfileRepository;

    public OnboardingApplicationService(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    public ProfileSummary savePrimaryGoal(String userKeyValue, String primaryGoalValue) {
        UserKey userKey = new UserKey(userKeyValue);
        PrimaryGoal primaryGoal = PrimaryGoal.fromContractValue(primaryGoalValue);
        return userProfileRepository.savePrimaryGoal(userKey, primaryGoal);
    }

    public ProfileSummary savePreferences(
            String userKeyValue,
            Integer dailyMinutes,
            String correctionStyle,
            Boolean reminderEnabled,
            Boolean saveRawText,
            Boolean saveRawAudio
    ) {
        UserKey userKey = new UserKey(userKeyValue);
        LearningPreferences preferences = LearningPreferences.fromContractValues(
                dailyMinutes,
                correctionStyle,
                reminderEnabled,
                saveRawText,
                saveRawAudio);
        return userProfileRepository.savePreferences(userKey, preferences);
    }

    public PrivacySettings getPrivacySettings(String userKeyValue) {
        return userProfileRepository.getPrivacySettings(new UserKey(userKeyValue));
    }

    public PrivacySettings savePrivacySettings(
            String userKeyValue,
            Boolean saveRawText,
            Boolean saveRawAudio,
            Integer rawAudioRetentionDays
    ) {
        PrivacySettings privacySettings = PrivacySettings.fromContractValues(
                saveRawText,
                saveRawAudio,
                rawAudioRetentionDays);
        return userProfileRepository.savePrivacySettings(new UserKey(userKeyValue), privacySettings);
    }

    public OnboardingProgress getProgress(String userKeyValue) {
        return userProfileRepository.getOnboardingProgress(new UserKey(userKeyValue));
    }
}
